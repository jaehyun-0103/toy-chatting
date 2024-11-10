package com.example.toychat.service;

import com.example.toychat.dto.request.InviteCodeCreateRequestDTO;
import com.example.toychat.dto.request.InviteCodeJoinRequestDTO;
import com.example.toychat.dto.response.InviteCodeCreateResponseDTO;
import com.example.toychat.dto.response.InviteCodeJoinResponseDTO;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.ChatRoomMember;
import com.example.toychat.entity.InviteCode;
import com.example.toychat.entity.User;

import com.example.toychat.repository.ChatRoomMemberRepository;
import com.example.toychat.repository.ChatRoomRepository;
import com.example.toychat.repository.InviteCodeRepository;
import com.example.toychat.repository.UserRepository;

import com.example.toychat.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class InviteCodeService {

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
     * 초대 코드 생성
     * @param token - JWT 토큰 (인증용)
     * @param requestDTO - 초대 코드 생성을 위한 요청 DTO
     * @return ResponseEntity - 초대 코드 생성 결과를 포함한 응답
     */
    public ResponseEntity<InviteCodeCreateResponseDTO> createInviteCode(String token, InviteCodeCreateRequestDTO requestDTO) {
        String username = jwtUtil.extractUsername(token);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User user = userOpt.get();

        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(requestDTO.getChatroomId());
        if (chatRoomOpt.isEmpty() || !chatRoomOpt.get().getCreator().equals(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new InviteCodeCreateResponseDTO("You are not authorized to generate an invite code for this chat room."));
        }

        ChatRoom chatRoom = chatRoomOpt.get();

        // 이미 초대 코드가 있는지 확인
        Optional<InviteCode> existingCode = inviteCodeRepository.findByChatRoom(chatRoom);
        if (existingCode.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new InviteCodeCreateResponseDTO("Invite code already exists for this chat room."));
        }

        // 6자리 랜덤 코드 생성
        String inviteCode = String.format("%06d", (int) (Math.random() * 1000000));

        InviteCode newInviteCode = new InviteCode();
        newInviteCode.setChatRoom(chatRoom);
        newInviteCode.setInviteCode(inviteCode);
        inviteCodeRepository.save(newInviteCode);

        return ResponseEntity.status(HttpStatus.CREATED).body(new InviteCodeCreateResponseDTO("Invite code generated successfully"));
    }

    /**
     * 초대 코드로 채팅방 참여
     * @param token - JWT 토큰 (인증용)
     * @param joinRequestDTO - 초대 코드로 채팅방 참여 요청 DTO
     * @return ResponseEntity - 참여 결과를 포함한 응답
     */
    public ResponseEntity<InviteCodeJoinResponseDTO> joinChatRoomUsingInviteCode(String token, InviteCodeJoinRequestDTO joinRequestDTO) {
        String username = jwtUtil.extractUsername(token);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User user = userOpt.get();

        // 초대 코드 조회
        Optional<InviteCode> inviteCodeOpt = inviteCodeRepository.findByInviteCode(joinRequestDTO.getInviteCode());
        if (inviteCodeOpt.isEmpty() || inviteCodeOpt.get().getExpirationDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new InviteCodeJoinResponseDTO("Invalid or expired invite code."));
        }

        ChatRoom chatRoom = inviteCodeOpt.get().getChatRoom();

        // 채팅방에 이미 참여한 경우
        Optional<ChatRoomMember> existingMember = chatRoomMemberRepository.findByChatRoomAndUser(chatRoom, user);
        if (existingMember.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new InviteCodeJoinResponseDTO("User is already a member of this chat room."));
        }

        // 초대 코드로 채팅방에 참여
        ChatRoomMember chatRoomMember = new ChatRoomMember();
        chatRoomMember.setChatRoom(chatRoom);
        chatRoomMember.setUser(user);
        chatRoomMemberRepository.save(chatRoomMember);

        return ResponseEntity.ok(new InviteCodeJoinResponseDTO("Joined chat room successfully using invite code"));
    }
}
