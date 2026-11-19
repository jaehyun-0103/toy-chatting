package com.example.toychat.security;

import com.example.toychat.entity.User;
import com.example.toychat.repository.ChatRoomMemberRepository;
import com.example.toychat.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompHandler implements ChannelInterceptor {

    private static final Pattern SUBSCRIBE_DESTINATION = Pattern.compile("^/topic/public/([1-9][0-9]*)$"); // 채팅방 구독 경로 정규식
    private static final Pattern SEND_DESTINATION = Pattern.compile("^/app/chat\\.sendMessage/([1-9][0-9]*)$"); // 채팅 메시지 전송 경로 정규식

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final ObjectProvider<MessageChannel> outboundChannel;

    private final Map<String, SessionUser> sessions = new ConcurrentHashMap<>(); // 세션 ID별 인증 사용자 정보 저장소

    /**
     * StompHandler 생성자입니다.
     *
     * @param jwtUtil          JWT 유틸리티
     * @param userRepository   사용자 리포지토리
     * @param memberRepository 채팅방 멤버 리포지토리
     * @param outboundChannel  클라이언트 아웃바운드 메시지 채널 제공자
     */
    public StompHandler(
            JwtUtil jwtUtil,
            UserRepository userRepository,
            ChatRoomMemberRepository memberRepository,
            @Qualifier("clientOutboundChannel")
            ObjectProvider<MessageChannel> outboundChannel
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.outboundChannel = outboundChannel;
    }

    /**
     * STOMP 인바운드 메시지가 채널로 전송되기 전에 인증 및 권한을 검증하고, 거부 시 ERROR 프레임을 전송합니다.
     *
     * @param message 전송될 메시지
     * @param channel 메시지 채널
     * @return 검증을 통과한 메시지, 거부된 경우 null
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try {
            return authorize(message); // 메시지 인증 및 권한 검증 수행
        } catch (AccessDeniedException ex) {
            StompHeaderAccessor incoming =
                    MessageHeaderAccessor.getAccessor(
                            message, StompHeaderAccessor.class
                    ); // 수신된 STOMP 헤더 접근자 추출

            if (incoming == null || incoming.getSessionId() == null) {
                throw ex;
            }

            String sessionId = incoming.getSessionId(); // WebSocket 세션 ID 추출
            sessions.remove(sessionId); // 세션 저장소에서 해당 세션 제거

            StompHeaderAccessor error = StompHeaderAccessor.create(StompCommand.ERROR); // STOMP ERROR 프레임 헤더 생성
            error.setSessionId(sessionId); // 에러 대상 세션 ID 설정
            error.setMessage("STOMP access denied"); // 에러 메시지 설정

            outboundChannel.getObject().send(MessageBuilder.createMessage(new byte[0], error.getMessageHeaders()));

            // 거부된 원본 프레임은 서비스와 브로커로 전달하지 않음
            return null;
        }
    }

    /**
     * STOMP 인바운드 메시지가 채널로 전송되기 전에 인증 및 권한을 검증합니다.
     *
     * @param message 전송될 메시지
     * @return 검증을 통과한 메시지
     */
    private Message<?> authorize(Message<?> message) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class); // STOMP 헤더 접근자 추출

        if (accessor == null) {
            throw denied();
        }

        StompCommand command = accessor.getCommand(); // STOMP 명령어 추출

        if (command == null) {
            if (accessor.getMessageType() == SimpMessageType.HEARTBEAT) {
                return message; // 하트비트 메시지는 검증 없이 통과
            }
            throw denied();
        }

        switch (command) {
            case CONNECT, STOMP -> authenticate(accessor); // 연결 요청 시 JWT 인증 수행

            case SUBSCRIBE -> {
                SessionUser user = requireUser(accessor); // 유효한 세션 사용자 조회
                Long roomId = roomId(accessor.getDestination(), SUBSCRIBE_DESTINATION); // 구독 대상 채팅방 ID 추출
                requireMember(roomId, user.userId()); // 채팅방 참여 여부 검증
            }

            case SEND -> {
                SessionUser user = requireUser(accessor); // 유효한 세션 사용자 조회
                Long roomId = roomId(accessor.getDestination(), SEND_DESTINATION); // 전송 대상 채팅방 ID 추출
                requireMember(roomId, user.userId()); // 채팅방 참여 여부 검증
            }

            case DISCONNECT, UNSUBSCRIBE -> {
                // 인증이 만료되어도 연결 종료와 구독 해제는 허용
            }

            default -> throw denied();
        }

        return message;
    }

    /**
     * STOMP 연결 요청 헤더의 JWT를 검증하고 세션에 사용자 인증 정보를 등록합니다.
     *
     * @param accessor STOMP 헤더 접근자
     */
    private void authenticate(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId(); // WebSocket 세션 ID 추출
        List<String> headers = accessor.getNativeHeader("Authorization"); // Authorization 헤더 조회

        if (sessionId == null
                || sessions.containsKey(sessionId)
                || headers == null
                || headers.size() != 1
                || !headers.get(0).startsWith("Bearer ")) {
            throw denied();
        }

        String token = headers.get(0).substring(7); // Bearer 접두사 제거 후 토큰 추출

        try {
            String username = jwtUtil.extractUsername(token); // 토큰에서 사용자 이름 추출
            Date expiration = jwtUtil.extractExpiration(token); // 토큰 만료 일시 추출

            if (expiration == null || !expiration.toInstant().isAfter(Instant.now())) {
                throw denied();
            }

            User user = userRepository.findByUsername(username).orElseThrow(StompHandler::denied); // 사용자 정보 조회

            var authentication = new UsernamePasswordAuthenticationToken(user.getId().toString(), null, List.of(new SimpleGrantedAuthority("USER"))); // Spring Security 인증 토큰 생성
            accessor.setUser(authentication); // STOMP 세션에 인증 객체 설정

            sessions.put(sessionId, new SessionUser(user.getId(), expiration.toInstant())); // 세션 저장소에 사용자 ID 및 만료 시간 등록
        } catch (JwtException | IllegalArgumentException ex) {
            throw denied();
        }
    }

    /**
     * 현재 세션의 사용자 인증 유효성 및 만료 여부를 확인합니다.
     *
     * @param accessor STOMP 헤더 접근자
     * @return 검증된 세션 사용자 정보
     */
    private SessionUser requireUser(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId(); // WebSocket 세션 ID 추출
        SessionUser user = sessionId == null ? null : sessions.get(sessionId); // 세션 저장소에서 사용자 조회

        if (user == null
                || !user.expiresAt().isAfter(Instant.now())
                || accessor.getUser() == null
                || !accessor.getUser().getName()
                .equals(user.userId().toString())
                || !userRepository.existsById(user.userId())) {
            throw denied();
        }

        return user;
    }

    /**
     * 사용자가 해당 채팅방의 멤버인지 검증합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    private void requireMember(Long roomId, Long userId) {
        if (!memberRepository.existsByChatRoom_IdAndUser_Id(roomId, userId)) {
            throw denied();
        }
    }

    /**
     * STOMP 목적지 경로에서 정규식을 이용해 채팅방 ID를 추출합니다.
     *
     * @param destination STOMP 목적지 경로
     * @param pattern     채팅방 ID 추출용 정규식 패턴
     * @return 추출된 채팅방 ID
     */
    private static Long roomId(String destination, Pattern pattern) {
        Matcher matcher = pattern.matcher(destination == null ? "" : destination); // 목적지 경로 정규식 매칭

        if (!matcher.matches()) {
            throw denied();
        }

        try {
            return Long.valueOf(matcher.group(1)); // 매칭된 그룹에서 채팅방 ID 파싱
        } catch (NumberFormatException ex) {
            throw denied();
        }
    }

    /**
     * 아웃바운드 메시지 전달 시점의 세션 유효성과 채팅방 멤버십을 검증하는 인터셉터를 반환합니다.
     *
     * @return 아웃바운드 채널 인터셉터
     */
    public ChannelInterceptor outboundInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message); // 메시지 헤더 접근자 래핑

                if (accessor.getMessageType() != SimpMessageType.MESSAGE) {
                    return message; // 일반 메시지가 아니면 검증 없이 통과
                }

                String sessionId = accessor.getSessionId(); // WebSocket 세션 ID 추출
                SessionUser user = sessionId == null ? null : sessions.get(sessionId); // 세션 저장소에서 사용자 조회

                if (user == null || !user.expiresAt().isAfter(Instant.now())) {
                    return null; // 세션이 없거나 만료된 경우 메시지 차단
                }

                Long roomId;

                try {
                    roomId = roomId(accessor.getDestination(),SUBSCRIBE_DESTINATION); // 목적지 경로에서 채팅방 ID 추출
                } catch (AccessDeniedException ex) {
                    return null;
                }

                // 기존 구독도 실제 전달 시점의 멤버십으로 재확인
                return memberRepository.existsByChatRoom_IdAndUser_Id(roomId, user.userId()) ? message : null;
            }
        };
    }

    /**
     * WebSocket 연결 종료 이벤트 발생 시 세션 저장소에서 해당 세션을 제거합니다.
     *
     * @param event 세션 연결 종료 이벤트
     */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        sessions.remove(event.getSessionId()); // 종료된 세션 ID 제거
    }

    /**
     * 접근 거부 예외 객체를 생성합니다.
     *
     * @return AccessDeniedException 객체
     */
    private static AccessDeniedException denied() {
        return new AccessDeniedException("STOMP access denied");
    }

    /**
     * 인증된 WebSocket 세션의 사용자 식별자와 토큰 만료 시각을 보관하는 레코드입니다.
     *
     * @param userId    사용자 ID
     * @param expiresAt 토큰 만료 시각
     */
    private record SessionUser(Long userId, Instant expiresAt) {    }
}