package com.example.toychat.service;

import com.example.toychat.dto.request.MessageSendRequestDTO;
import com.example.toychat.dto.request.MessageUpdateRequestDTO;
import com.example.toychat.dto.response.MessageResponseDTO;
import com.example.toychat.dto.response.MessageSendResponseDTO;
import com.example.toychat.dto.response.ResponseDTO;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.Message;
import com.example.toychat.entity.User;

import com.example.toychat.repository.ChatRoomMemberRepository;
import com.example.toychat.repository.ChatRoomRepository;
import com.example.toychat.repository.MessageRepository;
import com.example.toychat.repository.UserRepository;

import com.example.toychat.security.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil; // JWT 유틸리티 주입

    /**
     * 채팅방에서 사용자가 메시지를 전송합니다.
     *
     * @param token          사용자 인증 토큰
     * @param chatroomId     메시지를 보낼 채팅방 ID
     * @param sendRequestDTO 메시지 내용이 담긴 DTO
     * @return 전송 결과를 포함한 ResponseEntity
     */
    @Transactional
    public ResponseEntity<MessageSendResponseDTO> sendMessage(String token, Long chatroomId, MessageSendRequestDTO sendRequestDTO) {
        logger.info("Attempting to send message to chatting room ID: {} with content: {}", chatroomId, sendRequestDTO.getContent());

        // content가 비어 있는지 확인
        if (sendRequestDTO.getContent() == null || sendRequestDTO.getContent().trim().isEmpty()) {
            logger.warn("Invalid content in message send request for chatting room ID: {}", chatroomId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new MessageSendResponseDTO("Invalid content", null)
            );
        }

        // 토큰에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 조회
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageSendResponseDTO("User not found", null));
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getUsername());

        // 채팅방 조회
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(chatroomId);
        if (chatRoomOpt.isEmpty()) {
            logger.error("Chatting room not found for ID: {}", chatroomId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageSendResponseDTO("Chatting room not found", null));
        }
        ChatRoom chatRoom = chatRoomOpt.get();
        logger.info("Chatting room found: {} (ChatRoom ID: {})", chatRoom.getTitle(), chatRoom.getId());

        // 사용자가 채팅방의 멤버인지 확인
        boolean isMember = chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user);
        if (!isMember) {
            logger.warn("User {} is not a member of chatting room ID: {}", user.getUsername(), chatRoom.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageSendResponseDTO("User not a member of the chatting room", null));
        }

        // 메시지 생성 및 저장
        Message message = new Message();
        message.setChatRoom(chatRoom);
        message.setUser(user);
        message.setContent(sendRequestDTO.getContent());
        messageRepository.save(message);
        logger.info("Message sent successfully by user {} to chatting room ID: {} with content: {}", user.getUsername(), chatRoom.getId(), sendRequestDTO.getContent());

        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageSendResponseDTO("Message sent successfully", message.getId()));
    }

    /**
     * 사용자가 해당 채팅방 전체 메시지를 조회합니다.
     *
     * @param token      사용자 인증 토큰
     * @param chatroomId 메시지를 조회할 채팅방 ID
     * @return 메시지 리스트를 포함한 ResponseEntity
     */
    public ResponseEntity<List<MessageResponseDTO>> getMessages(String token, Long chatroomId) {
        logger.info("Attempting to get messages for chatting room ID: {}", chatroomId);

        // 토큰에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 조회
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for username: {}", username);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getUsername());

        // 채팅방 조회
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(chatroomId);
        if (chatRoomOpt.isEmpty()) {
            logger.error("Chatting room not found for ID: {}", chatroomId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chatting room not found");
        }
        ChatRoom chatRoom = chatRoomOpt.get();
        logger.info("Chatting room found: {} (ChatRoom ID: {})", chatRoom.getTitle(), chatRoom.getId());

        // 사용자가 채팅방의 멤버인지 확인
        boolean isMember = chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user);
        if (!isMember) {
            logger.warn("User {} is not a member of chatting room ID: {}", user.getUsername(), chatRoom.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not a member of the chatting room");
        }
        logger.info("User {} is a member of chatting room ID: {}", user.getUsername(), chatRoom.getId());

        // 모든 메시지 조회
        List<Message> messages = messageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);
        logger.info("Found {} messages in chatting room ID: {}", messages.size(), chatRoom.getId());

        // request
        List<MessageResponseDTO> response = messages.stream()
                .map(msg -> new MessageResponseDTO(msg.getId(), msg.getUser().getUsername(), msg.getContent(), msg.getUpdatedAt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 사용자가 전송한 메시지를 수정합니다.
     *
     * @param token            사용자 인증 토큰
     * @param chatroomId       수정할 메시지가 포함된 채팅방 ID
     * @param messageId        수정할 메시지의 ID
     * @param updateRequestDTO 수정할 내용이 담긴 DTO
     * @return 수정 결과를 포함한 ResponseEntity
     */
    @Transactional
    public ResponseEntity<ResponseDTO> updateMessage(String token, Long chatroomId, Long messageId, MessageUpdateRequestDTO updateRequestDTO) {
        logger.info("Attempting to update message ID: {} in chatting room ID: {}", messageId, chatroomId);

        // content가 비어 있는지 확인
        if (updateRequestDTO.getContent() == null || updateRequestDTO.getContent().trim().isEmpty()) {
            logger.error("Content is null for message update request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ResponseDTO("Invalid content")
            );
        }

        // 토큰에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 조회
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("User not found"));
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getUsername());

        // 메시지 조회
        Optional<Message> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            logger.error("Message not found for ID: {}", messageId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("Message not found"));
        }
        Message message = messageOpt.get();
        logger.info("Message found: {} (Message ID: {})", message.getContent(), message.getId());

        // 사용자가 해당 메시지의 작성자인지 확인
        if (!message.getUser().equals(user)) {
            logger.warn("User {} is not authorized to edit message ID: {}", user.getUsername(), messageId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseDTO("User not authorized to edit this message"));
        }
        logger.info("User {} is authorized to edit message ID: {}", user.getUsername(), messageId);

        // 메시지 내용 수정
        message.setContent(updateRequestDTO.getContent());
        messageRepository.save(message);
        logger.info("Message ID: {} updated successfully", messageId);

        return ResponseEntity.ok(new ResponseDTO("Message updated successfully"));
    }
}
