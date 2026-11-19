package com.example.toychat.service;

import com.example.toychat.dto.request.MessageSendRequestDTO;
import com.example.toychat.dto.request.MessageUpdateRequestDTO;
import com.example.toychat.dto.response.MessageResponseDTO;
import com.example.toychat.dto.response.ResponseDTO;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.Message;
import com.example.toychat.entity.User;

import com.example.toychat.repository.ChatRoomMemberRepository;
import com.example.toychat.repository.ChatRoomRepository;
import com.example.toychat.repository.MessageRepository;
import com.example.toychat.repository.UserRepository;

import com.example.toychat.security.JwtUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

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

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 채팅방에서 사용자가 메시지를 전송합니다.
     *
     * @param userId         사용자 ID
     * @param chatroomId     메시지를 보낼 채팅방 ID
     * @param sendRequestDTO 메시지 내용이 담긴 DTO
     * @return 전송 결과를 포함한 ResponseEntity
     */
    public MessageResponseDTO sendMessage(Long userId, Long chatroomId, MessageSendRequestDTO sendRequestDTO) {
        logger.info("Message send requested; userId={} roomId={}", userId, chatroomId);

        // content가 비어 있는지 확인
        if (sendRequestDTO.getContent() == null || sendRequestDTO.getContent().trim().isEmpty()) {
            logger.warn("Invalid content in message send request for chatting room ID: {}", chatroomId);
            throw new MessagingException("Invalid content");
        }

        // 사용자 조회
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.error("User not found for ID: {}", userId);
            throw new MessagingException("User not found");
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getId());

        // 채팅방 조회
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(chatroomId);
        if (chatRoomOpt.isEmpty()) {
            logger.error("Chatting room not found for ID: {}", chatroomId);
            throw new MessagingException("Chat room not found");
        }
        ChatRoom chatRoom = chatRoomOpt.get();
        logger.info("Chatting room found; roomId={}", chatRoom.getId());

        // 사용자가 채팅방의 멤버인지 확인
        boolean isMember = chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user);
        if (!isMember) {
            logger.warn("User {} is not a member of chatting room ID: {}", user.getId(), chatRoom.getId());
            throw new MessagingException("User is not a member of the chat room");
        }

        // 메시지 생성 및 저장
        Message message = new Message();
        message.setChatRoom(chatRoom);
        message.setUser(user);
        message.setContent(sendRequestDTO.getContent());
        messageRepository.save(message);
        logger.info("MESSAGE_SENT userId={} roomId={} messageId={}", user.getId(), chatRoom.getId(), message.getId());

        // response
        MessageResponseDTO responseDTO = new MessageResponseDTO(
                message.getId(),
                user.getUsername(),
                user.getId(),
                message.getContent(),
                message.getUpdatedAt()
        );

        return responseDTO;
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

        // 토큰에서 사용자 ID 추출
        Long userId = jwtUtil.extractUserId(token);
        logger.debug("Extracted user ID from JWT: {}", userId);

        // 사용자 조회
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.error("User not found for ID: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getId());

        // 채팅방 조회
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(chatroomId);
        if (chatRoomOpt.isEmpty()) {
            logger.error("Chatting room not found for ID: {}", chatroomId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chatting room not found");
        }
        ChatRoom chatRoom = chatRoomOpt.get();
        logger.info("Chatting room found; roomId={}", chatRoom.getId());

        // 사용자가 채팅방의 멤버인지 확인
        boolean isMember = chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user);
        if (!isMember) {
            logger.warn("User {} is not a member of chatting room ID: {}", user.getId(), chatRoom.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not a member of the chatting room");
        }
        logger.info("User {} is a member of chatting room ID: {}", user.getId(), chatRoom.getId());

        // 모든 메시지 조회
        List<MessageResponseDTO> response = messageRepository.findMessageResponsesByChatRoomId(chatRoom.getId());
        logger.info("Found {} messages in chatting room ID: {}", response.size(), chatRoom.getId());

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
            return ResponseEntity.badRequest().body(new ResponseDTO("Invalid content"));
        }

        // 토큰에서 사용자 ID 추출
        Long userId = jwtUtil.extractUserId(token);
        logger.debug("Extracted user ID from JWT: {}", userId);

        // 사용자 조회
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.error("User not found for ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("User not found"));
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getId());

        // 메시지 조회
        Optional<Message> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            logger.error("Message not found for ID: {}", messageId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("Message not found"));
        }
        Message message = messageOpt.get();
        Long actualRoomId = message.getChatRoom().getId();
        logger.info("Message found; messageId={} roomId={}", message.getId(), actualRoomId);

        // 해당 채팅방의 메시지인지 확인
        if (!actualRoomId.equals(chatroomId)) {
            logger.warn("Message ID: {} does not belong to chatting room ID: {}", messageId, chatroomId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("Message not found in this chatting room"));
        }

        // 사용자가 채팅방의 멤버인지 확인
        if (!chatRoomMemberRepository.existsByChatRoomAndUser(message.getChatRoom(), user)) {
            logger.warn("User {} is not a member of chatting room ID: {}", user.getId(), actualRoomId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseDTO("User is not a member of this chatting room"));
        }

        // 사용자가 해당 메시지의 작성자인지 확인
        if (!message.getUser().getId().equals(user.getId())) {
            logger.warn("User {} is not authorized to edit message ID: {}", user.getId(), messageId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseDTO("User not authorized to edit this message"));
        }
        logger.info("User {} is authorized to edit message ID: {}", user.getId(), messageId);

        // 메시지 내용 수정
        message.setContent(updateRequestDTO.getContent());
        messageRepository.saveAndFlush(message);

        // DB 저장 시각 반영을 위해 엔티티 새로고침
        entityManager.refresh(message);

        // 커밋 후 전송을 위한 응답 DTO 스냅샷 생성
        MessageResponseDTO snapshot = new MessageResponseDTO(
                message.getId(),
                user.getUsername(),
                user.getId(),
                message.getContent(),
                message.getUpdatedAt()
        );

        // 트랜잭션 커밋 후 메시지 브로드캐스트
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        logger.info("MESSAGE_UPDATED userId={} roomId={} messageId={}", snapshot.getUserId(), actualRoomId, snapshot.getMessageId());

                        try {
                            messagingTemplate.convertAndSend("/topic/public/" + actualRoomId,snapshot);
                        } catch (RuntimeException ex) {
                            // DB 커밋 이후 브로드캐스트 실패 로그 기록
                            logger.warn("MESSAGE_UPDATE_BROADCAST_FAILED roomId={} messageId={}", actualRoomId, snapshot.getMessageId());
                        }
                    }
                }
        );

        return ResponseEntity.ok(new ResponseDTO("Message updated successfully"));
    }
}
