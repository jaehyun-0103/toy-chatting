package com.example.toychat.service;

import com.example.toychat.dto.request.MessageSendRequestDTO;
import com.example.toychat.dto.request.MessageUpdateRequestDTO;
import com.example.toychat.dto.response.CursorPageResponseDTO;
import com.example.toychat.dto.response.MessageResponseDTO;
import com.example.toychat.dto.response.MessageUpdateResponseDTO;
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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
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
     * 사용자가 해당 채팅방의 메시지를 커서 기반으로 조회합니다.
     *
     * @param token      사용자 인증 토큰
     * @param chatroomId 메시지를 조회할 채팅방 ID
     * @param beforeId   기준 메시지 ID (null일 경우 최신 메시지 조회)
     * @param size       조회할 메시지 개수
     * @return 커서 기반 메시지 페이지 응답을 포함한 ResponseEntity
     */
    public ResponseEntity<CursorPageResponseDTO<MessageResponseDTO>> getMessages(String token, Long chatroomId, Long beforeId, int size) {
        logger.info("Attempting to get messages for chatting room ID: {}", chatroomId);

        // 채팅방 ID 유효성 확인
        if (chatroomId == null || chatroomId <= 0) {
            logger.warn("Invalid chatting room ID: {}", chatroomId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CHATROOM_ID");
        }

        // 페이지 크기 유효성 확인
        if (size < 1 || size > 100) {
            logger.warn("Invalid page size: {}", size);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_SIZE");
        }

        // 커서 ID 유효성 확인
        if (beforeId != null && beforeId <= 0) {
            logger.warn("Invalid cursor ID: {}", beforeId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR");
        }

        // 토큰에서 사용자 ID 추출
        Long userId = jwtUtil.extractUserId(token);
        logger.debug("Extracted user ID from JWT: {}", userId);

        // 사용자 조회
        User user = userRepository.findById(userId).orElseThrow(() -> {
            logger.error("User not found for ID: {}", userId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        });
        logger.info("User found: {}", user.getId());

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatroomId).orElseThrow(() -> {
            logger.error("Chatting room not found for ID: {}", chatroomId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Chatting room not found");
        });
        logger.info("Chatting room found; roomId={}", chatRoom.getId());

        // 사용자가 채팅방의 멤버인지 확인
        if (!chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user)) {
            logger.warn("User {} is not a member of chatting room ID: {}", user.getId(), chatRoom.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not a member of the chatting room");
        }
        logger.info("User {} is a member of chatting room ID: {}", user.getId(), chatRoom.getId());

        // 페이지 요청 생성
        PageRequest pageable = PageRequest.of(0, size);

        // 커서 유무에 따라 최신 또는 이전 메시지 조회
        Slice<MessageResponseDTO> slice = beforeId == null
                ? messageRepository.findRecentMessageResponses(
                chatroomId, pageable
        )
                : messageRepository.findOlderMessageResponses(
                chatroomId, beforeId, pageable
        );

        List<MessageResponseDTO> items = slice.getContent();

        // 다음 페이지 조회를 위한 커서 계산
        Long nextCursor = slice.hasNext() && !items.isEmpty() ? items.get(items.size() - 1).getMessageId() : null;
        logger.info("Message page returned; userId={} roomId={} count={} hasNext={}", userId, chatroomId, items.size(), slice.hasNext());

        return ResponseEntity.ok(new CursorPageResponseDTO<>(items, nextCursor, slice.hasNext()));
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

        return ResponseEntity.ok(new MessageUpdateResponseDTO("Message updated successfully", snapshot));
    }
}
