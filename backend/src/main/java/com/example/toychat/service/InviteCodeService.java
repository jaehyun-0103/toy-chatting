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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.toychat.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class InviteCodeService {

    private static final Logger logger = LoggerFactory.getLogger(InviteCodeService.class);

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

    /**
     * 채팅방 생성자가 초대 코드를 생성합니다.
     *
     * @param token            JWT 토큰 (인증용)
     * @param createRequestDTO 초대 코드 생성을 위한 요청 DTO
     * @return ResponseEntity  초대 코드 생성 결과를 포함한 응답
     */
    public ResponseEntity<InviteCodeCreateResponseDTO> createInviteCode(String token, InviteCodeCreateRequestDTO createRequestDTO) {
        logger.info("Attempting to create an invite code for chatting room ID: {}", createRequestDTO.getChatroomId());

        // chatroom_id가 비어 있는지 확인
        if (createRequestDTO.getChatroomId() == null) {
            logger.warn("Invalid chatroom_id in request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new InviteCodeCreateResponseDTO("Invalid chatroom_id", null)
            );
        }

        // 토큰에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new InviteCodeCreateResponseDTO("User not found", null));
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getUsername());

        // 채팅방 찾기
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(createRequestDTO.getChatroomId());
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
            logger.warn("Invite code already exists for chatting room ID: {}", chatRoom.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new InviteCodeCreateResponseDTO("Invite code already exists for this chatting room", null));
        }

        // 6자리 랜덤 코드 생성
        String inviteCode = String.format("%06d", (int) (Math.random() * 1000000));
        logger.debug("Generated invite code: {}", inviteCode);

        // 초대코드 정보 추가
        InviteCode newInviteCode = new InviteCode();
        newInviteCode.setChatRoom(chatRoom);
        newInviteCode.setInviteCode(inviteCode);
        inviteCodeRepository.save(newInviteCode);
        logger.info("Invite code {} created successfully for chatting room ID: {}", inviteCode, chatRoom.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new InviteCodeCreateResponseDTO("Invite code created successfully", inviteCode));
    }

    /**
     * 초대 코드로 채팅방에 참여합니다.
     *
     * @param token          JWT 토큰 (인증용)
     * @param joinRequestDTO 초대 코드로 채팅방 참여 요청 DTO
     * @return ResponseEntity 참여 결과를 포함한 응답
     */
    public ResponseEntity<ResponseDTO> joinByInviteCode(String token, InviteCodeJoinRequestDTO joinRequestDTO) {
        logger.info("Attempting to join chatting room using invite code: {}", joinRequestDTO.getInviteCode());

        // invite_code가 비어 있는지 확인
        if (joinRequestDTO.getInviteCode() == null) {
            logger.warn("Invalid invite_code in request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ResponseDTO("Invalid invite_code")
            );
        }

        // 토큰에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("User not found"));
        }
        User user = userOpt.get();

        // 초대 코드 조회
        Optional<InviteCode> inviteCodeOpt = inviteCodeRepository.findByInviteCode(joinRequestDTO.getInviteCode());
        if (inviteCodeOpt.isEmpty() || inviteCodeOpt.get().getExpirationDate().isBefore(LocalDateTime.now())) {
            logger.warn("Invalid or expired invite code: {}", joinRequestDTO.getInviteCode());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("Invalid or expired invite code"));
        }
        ChatRoom chatRoom = inviteCodeOpt.get().getChatRoom();
        logger.info("Chatting room found for invite code: {} (ChatRoom ID: {})", joinRequestDTO.getInviteCode(), chatRoom.getId());

        // 사용자가 채팅방의 멤버인지 확인
        boolean isMember = chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user);
        if (isMember) {
            logger.warn("User {} is already a member of chatting room ID: {}", user.getUsername(), chatRoom.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseDTO("User is already a member of this chatting room"));
        }

        // 초대 코드로 채팅방에 참여
        ChatRoomMember chatRoomMember = new ChatRoomMember();
        chatRoomMember.setChatRoom(chatRoom);
        chatRoomMember.setUser(user);
        chatRoomMemberRepository.save(chatRoomMember);
        logger.info("User {} successfully joined chatting room ID: {} using invite code", user.getUsername(), chatRoom.getId());

        return ResponseEntity.ok(new ResponseDTO("Joined chatting room successfully using invite code"));
    }
}
