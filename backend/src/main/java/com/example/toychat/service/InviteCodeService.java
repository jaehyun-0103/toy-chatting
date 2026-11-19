package com.example.toychat.service;

import com.example.toychat.dto.request.InviteCodeCreateRequestDTO;
import com.example.toychat.dto.request.InviteCodeJoinRequestDTO;
import com.example.toychat.dto.response.InviteCodeCreateResponseDTO;
import com.example.toychat.dto.response.ResponseDTO;
import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.ChatRoomMember;
import com.example.toychat.entity.InviteCode;
import com.example.toychat.entity.User;
import com.example.toychat.repository.ChatRoomMemberRepository;
import com.example.toychat.repository.ChatRoomRepository;
import com.example.toychat.repository.InviteCodeRepository;
import com.example.toychat.repository.UserRepository;
import com.example.toychat.security.InviteJoinRateLimiter;
import com.example.toychat.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class InviteCodeService {

    private static final Logger logger = LoggerFactory.getLogger(InviteCodeService.class);

    private static final String CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; // 초대 코드 생성에 사용할 문자 집합

    private final SecureRandom secureRandom = new SecureRandom(); // 난수 생성기

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil; // JWT 유틸리티 주입

    @Autowired
    private InviteJoinRateLimiter inviteJoinRateLimiter; // 초대 코드 참여 속도 제한기 주입

    /**
     * 채팅방 생성자가 초대 코드를 생성합니다.
     *
     * @param token            JWT 토큰 (인증용)
     * @param createRequestDTO 초대 코드 생성을 위한 요청 DTO
     * @return ResponseEntity  초대 코드 생성 결과를 포함한 응답
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<InviteCodeCreateResponseDTO> createInviteCode(String token, InviteCodeCreateRequestDTO createRequestDTO) {
        logger.info("Attempting to create an invite code for chatting room ID: {}", createRequestDTO.getChatroomId());

        // chatroom_id가 비어 있는지 확인
        if (createRequestDTO.getChatroomId() == null) {
            logger.warn("Invalid chatroom_id in request");
            return ResponseEntity.badRequest().body(new InviteCodeCreateResponseDTO("Invalid chatroom_id", null));
        }

        // 토큰에서 사용자 ID 추출
        Long userId = jwtUtil.extractUserId(token);
        logger.debug("Extracted user ID from JWT: {}", userId);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.error("User not found for ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new InviteCodeCreateResponseDTO("User not found", null));
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getUsername());

        // 채팅방 찾기 (비관적 락을 적용하여 같은 방의 발급 요청 직렬화)
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findByIdWithLock(createRequestDTO.getChatroomId());

        if (chatRoomOpt.isEmpty()) {
            logger.error("Chatting room not found for ID: {}", createRequestDTO.getChatroomId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new InviteCodeCreateResponseDTO("Chatting room not found", null));
        }
        ChatRoom chatRoom = chatRoomOpt.get();
        logger.info("Chatting room found: {}", chatRoom.getId());

        // 채팅방 생성자인지 확인
        if (!chatRoom.getCreator().equals(user)) {
            logger.warn("User {} is not the creator of chatting room {}", user.getUsername(), chatRoom.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new InviteCodeCreateResponseDTO("User is not the creator of this chatting room", null));
        }

        // 이미 초대 코드가 있는지 확인
        Optional<InviteCode> existingCode = inviteCodeRepository.findByChatRoom(chatRoom);
        if (existingCode.isPresent()) {
            // 기존 초대 코드의 만료 여부 확인
            boolean expired = !existingCode.get().getExpirationDate().isAfter(LocalDateTime.now());

            if (expired) {
                logger.warn("Expired invite code is waiting for cleanup for chatting room ID: {}", chatRoom.getId());
                return ResponseEntity.badRequest().body(new InviteCodeCreateResponseDTO("Expired invite code is waiting for cleanup. Please retry in about one minute.", null));
            }

            logger.warn("Invite code already exists for chatting room ID: {}", chatRoom.getId());
            return ResponseEntity.badRequest().body(new InviteCodeCreateResponseDTO("Invite code already exists for this chatting room", null));
        }

        // 8자리 랜덤 코드 생성
        String code = generateInviteCode();
        logger.debug("Generated invite code: {}", code);

        // 초대코드 정보 추가
        InviteCode newInviteCode = new InviteCode();
        newInviteCode.setChatRoom(chatRoom);
        newInviteCode.setInviteCode(code);

        try {
            // 저장 시점에 유일 제약 위반 확인
            inviteCodeRepository.saveAndFlush(newInviteCode);
        } catch (DataIntegrityViolationException ex) {
            logger.error("Duplicate invite code generated for chatting room ID: {}", chatRoom.getId());
            // 예외를 다시 던져 트랜잭션 롤백
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "INVITE_CODE_GENERATION_RETRY");
        }

        logger.info("Invite code {} created successfully for chatting room ID: {}", code, chatRoom.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new InviteCodeCreateResponseDTO("Invite code created successfully", code));
    }

    /**
     * 초대 코드로 채팅방에 참여합니다.
     *
     * @param token          JWT 토큰 (인증용)
     * @param joinRequestDTO 초대 코드로 채팅방 참여 요청 DTO
     * @return ResponseEntity 참여 결과를 포함한 응답
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<ResponseDTO> joinByInviteCode(String token, InviteCodeJoinRequestDTO joinRequestDTO) {
        logger.info("Attempting to join chatting room using invite code: {}", joinRequestDTO.getInviteCode());

        // invite_code가 비어 있는지 확인
        if (joinRequestDTO.getInviteCode() == null) {
            logger.warn("Invalid invite_code in request");
            return ResponseEntity.badRequest().body(new ResponseDTO("Invalid invite_code"));
        }

        // 토큰에서 사용자 ID 추출
        Long userId = jwtUtil.extractUserId(token);
        logger.debug("Extracted user ID from JWT: {}", userId);

        // 요청 속도 제한 여부 확인
        long retryAfter = inviteJoinRateLimiter.retryAfterIfLimited(userId);

        if (retryAfter > 0) {
            logger.warn("Rate limit exceeded for user ID: {}. Retry after {} seconds", userId, retryAfter);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", Long.toString(retryAfter)).body(new ResponseDTO("TOO_MANY_REQUESTS"));
        }

        // 초대 코드 형식 검증 (기존 숫자 6자리 및 신규 영문·숫자 8자리 허용)
        String code = joinRequestDTO.getInviteCode().trim().toUpperCase(Locale.ROOT);

        if (!code.matches("(?:[0-9]{6}|[A-Z0-9]{8})")) {
            logger.warn("Invalid invite code format: {}", code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("Invalid or expired invite code"));
        }

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.error("User not found for ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("User not found"));
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getUsername());

        // 초대 코드 조회 및 대상 채팅방 ID 확인
        Optional<InviteCode> inviteCodeOpt = inviteCodeRepository.findByInviteCode(code);

        if (inviteCodeOpt.isEmpty()) {
            logger.warn("Invalid or expired invite code: {}", code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("Invalid or expired invite code"));
        }

        // 채팅방 찾기 (비관적 락 적용)
        Long chatroomId = inviteCodeOpt.get().getChatRoom().getId();
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findByIdWithLock(chatroomId);

        if (chatRoomOpt.isEmpty()) {
            logger.error("Chatting room not found for ID: {}", chatroomId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("Chatting room not found"));
        }

        ChatRoom chatRoom = chatRoomOpt.get();
        logger.info("Chatting room found for invite code: {} (ChatRoom ID: {})", code, chatRoom.getId());

        // 초대 코드 유효성 및 만료 여부 재확인
        Optional<InviteCode> currentCode = inviteCodeRepository.findByInviteCode(code);

        if (currentCode.isEmpty() || !currentCode.get().getChatRoom().getId().equals(chatRoom.getId()) || !currentCode.get().getExpirationDate().isAfter(LocalDateTime.now())) {
            logger.warn("Invalid or expired invite code after lock acquisition: {}", code);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("Invalid or expired invite code"));
        }

        // 사용자가 채팅방의 멤버인지 확인
        if (chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user)) {
            logger.warn("User {} is already a member of chatting room ID: {}", user.getUsername(), chatRoom.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseDTO("User is already a member of this chatting room"));
        }

        // 채팅방 최대 인원 확인
        int memberCount = chatRoomMemberRepository.countByChatRoom(chatRoom);
        if (chatRoom.isFull(memberCount)) {
            logger.warn("Chatting room {} is full. Current members: {}, Max members: {}", chatRoom.getId(), memberCount, chatRoom.getMaxMembers());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseDTO("ROOM_MAX_CAPACITY_EXCEEDED"));
        }

        // 초대 코드로 채팅방에 참여
        ChatRoomMember member = new ChatRoomMember();
        member.setChatRoom(chatRoom);
        member.setUser(user);
        chatRoomMemberRepository.save(member);
        logger.info("User {} successfully joined chatting room ID: {} using invite code", user.getUsername(), chatRoom.getId());

        return ResponseEntity.ok(new ResponseDTO("Joined chatting room successfully using invite code"));
    }

    /**
     * 영문 대문자와 숫자로 구성된 8자리 랜덤 초대 코드를 생성합니다.
     *
     * @return 생성된 8자리 초대 코드 문자열
     */
    private String generateInviteCode() {
        StringBuilder code = new StringBuilder(8); // 8자리 초대 코드 버퍼 생성

        for (int i = 0; i < 8; i++) {
            code.append(CODE_CHARACTERS.charAt(secureRandom.nextInt(CODE_CHARACTERS.length()))); // 허용된 문자 집합에서 무작위 문자 추가
        }

        return code.toString();
    }
}