package com.example.toychat.service;

import com.example.toychat.dto.request.MessageSendRequestDTO;
import com.example.toychat.dto.request.MessageUpdateRequestDTO;
import com.example.toychat.dto.response.MessageResponseDTO;
import com.example.toychat.dto.response.MessageSendResponseDTO;
import com.example.toychat.dto.response.MessageUpdateResponseDTO;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.Message;
import com.example.toychat.entity.User;

import com.example.toychat.repository.ChatRoomMemberRepository;
import com.example.toychat.repository.ChatRoomRepository;
import com.example.toychat.repository.MessageRepository;
import com.example.toychat.repository.UserRepository;

import com.example.toychat.security.JwtUtil;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 특정 채팅방에서 사용자가 메시지를 전송합니다.
     * @param token 사용자 인증 토큰
     * @param chatroomId 메시지를 보낼 채팅방 ID
     * @param requestDTO 메시지 내용이 담긴 DTO
     * @return 전송 결과를 포함한 ResponseEntity
     */
    @Transactional
    public ResponseEntity<MessageSendResponseDTO> sendMessage(String token, Long chatroomId, MessageSendRequestDTO requestDTO) {
        // 토큰에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);

        // 사용자 조회
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found"));

        // 사용자가 채팅방의 멤버인지 확인
        boolean isMember = chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user);
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not a member of the chat room");
        }

        // 메시지 생성 및 저장
        Message message = new Message();
        message.setChatRoom(chatRoom);
        message.setUser(user);
        message.setContent(requestDTO.getContent());
        messageRepository.save(message);

        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageSendResponseDTO(message.getId(), "Message sent successfully"));
    }

    /**
     * 채팅방 사용자가 해당 채팅방 전체 메시지를 조회합니다.
     * @param token 사용자 인증 토큰
     * @param chatroomId 메시지를 조회할 채팅방 ID
     * @return 메시지 리스트를 포함한 ResponseEntity
     */
    public ResponseEntity<List<MessageResponseDTO>> getMessages(String token, Long chatroomId) {
        // 토큰에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);

        // 사용자 조회
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat room not found"));

        // 사용자가 채팅방의 멤버인지 확인
        boolean isMember = chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user);
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not a member of the chat room");
        }

        // 메시지 조회 및 DTO 변환
        List<Message> messages = messageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);
        List<MessageResponseDTO> response = messages.stream()
                .map(msg -> new MessageResponseDTO(msg.getId(), msg.getUser().getUsername(), msg.getContent(), msg.getUpdatedAt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 사용자가 본인이 전송한 메시지를 수정합니다.
     * @param token 사용자 인증 토큰
     * @param chatroomId 수정할 메시지가 포함된 채팅방 ID
     * @param messageId 수정할 메시지의 ID
     * @param requestDTO 수정할 내용이 담긴 DTO
     * @return 수정 결과를 포함한 ResponseEntity
     */
    @Transactional
    public ResponseEntity<MessageUpdateResponseDTO> updateMessage(String token, Long chatroomId, Long messageId, MessageUpdateRequestDTO requestDTO) {
        // 토큰에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);

        // 사용자 조회
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 메시지 조회
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        // 사용자가 해당 메시지의 작성자인지 확인
        if (!message.getUser().equals(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not authorized to edit this message");
        }

        // 메시지 내용 수정 및 저장
        message.setContent(requestDTO.getContent());
        messageRepository.save(message);

        return ResponseEntity.ok(new MessageUpdateResponseDTO("Message updated successfully"));
    }
}
