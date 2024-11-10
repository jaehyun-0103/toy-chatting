package com.example.toychat.service;

import com.example.toychat.dto.request.ChatRoomJoinRequestDTO;
import com.example.toychat.dto.response.*;
import com.example.toychat.dto.request.ChatRoomCreateRequestDTO;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.ChatRoomMember;
import com.example.toychat.entity.User;

import com.example.toychat.repository.ChatRoomMemberRepository;
import com.example.toychat.repository.ChatRoomRepository;
import com.example.toychat.repository.UserRepository;

import com.example.toychat.security.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomService.class);

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil; // JWT 유틸리티 주입

    /**
     * 채팅방을 생성합니다.
     *
     * @param token      JWT 토큰
     * @param requestDTO 채팅방 생성 요청 데이터
     * @return 채팅방 생성 결과
     */
    public ResponseEntity<ChatRoomCreateResponseDTO> createChatRoom(String token, ChatRoomCreateRequestDTO requestDTO) {
        logger.info("Attempting to create a chatting room with title: {} and max members: {}", requestDTO.getTitle(), requestDTO.getMaxMembers());

        // 제목 또는 최대 인원이 비어 있는지 확인
        if (requestDTO.getTitle() == null || requestDTO.getTitle().isEmpty() ||
                requestDTO.getMaxMembers() == null) {
            logger.warn("Invalid input detected: Title or Max Members is missing.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ChatRoomCreateResponseDTO("Invalid title or max_members value", null, null)
            );
        }

        // 최대 인원 제한 확인 (1~20 사이 값만 허용)
        if (requestDTO.getMaxMembers() < 1 || requestDTO.getMaxMembers() > 20) {
            logger.warn("Invalid max members value: {}", requestDTO.getMaxMembers());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ChatRoomCreateResponseDTO("Max_members must be between 1 and 20", null, null)
            );
        }

        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ChatRoomCreateResponseDTO("User not found", null, null));
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getUsername());

        // 채팅방 생성
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setTitle(requestDTO.getTitle());
        chatRoom.setMaxMembers(requestDTO.getMaxMembers());
        chatRoom.setPrivate(requestDTO.isPrivate());
        chatRoom.setCreator(user);
        chatRoomRepository.save(chatRoom);
        logger.info("Chatting room created with ID: {}", chatRoom.getId());

        // 생성자를 채팅방의 참여자로 추가
        ChatRoomMember chatRoomMember = new ChatRoomMember();
        chatRoomMember.setChatRoom(chatRoom);
        chatRoomMember.setUser(user);
        chatRoomMemberRepository.save(chatRoomMember);
        logger.info("User {} added as a member to the chatting room.", user.getUsername());

        // response
        ChatRoomCreateResponseDTO responseDTO = new ChatRoomCreateResponseDTO(
                "Chatting room created successfully",
                chatRoom.getId(),
                user.getId()
        );

        logger.info("Chatting room creation successful, returning response with chatting room ID: {}", chatRoom.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    /**
     * 채팅방에 참여합니다.
     *
     * @param token          JWT 토큰
     * @param joinRequestDTO 채팅방 참여 요청 데이터
     * @return 채팅방 참여 결과
     */
    public ResponseEntity<ResponseDTO> joinChatRoom(String token, ChatRoomJoinRequestDTO joinRequestDTO) {
        logger.info("Attempting to join chatting room with ID: {}", joinRequestDTO.getChatroomId());
        // chatroom_id가 비어 있는지 확인
        if (joinRequestDTO.getChatroomId() == null) {
            logger.warn("Invalid chatroom ID received: {}", joinRequestDTO.getChatroomId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ResponseDTO("Invalid chatroom_id")
            );
        }

        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("User not found"));
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getUsername());

        // 채팅방 찾기
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(joinRequestDTO.getChatroomId());
        if (chatRoomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("Chatting room not found"));
        }
        ChatRoom chatRoom = chatRoomOpt.get();
        logger.info("Chatting room found: {}", chatRoom.getId());

        // 채팅방에 이미 참여한 사용자인지 확인
        boolean isMember = chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user);
        if (isMember) {
            logger.warn("User {} is already a member of chatting room {}", user.getUsername(), chatRoom.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseDTO("User is already a member of this chatting room"));
        }

        // 채팅방 최대 인원 확인
        if (chatRoom.getMembers().size() >= chatRoom.getMaxMembers()) {
            logger.warn("Chatting room {} is full. Current members: {}, Max members: {}", chatRoom.getId(), chatRoom.getMembers().size(), chatRoom.getMaxMembers());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseDTO("Chatting room is full"));
        }

        // 채팅방에 사용자 추가
        ChatRoomMember chatRoomMember = new ChatRoomMember();
        chatRoomMember.setChatRoom(chatRoom);
        chatRoomMember.setUser(user);
        chatRoomMemberRepository.save(chatRoomMember);
        logger.info("User {} successfully joined chatting room {}", user.getUsername(), chatRoom.getId());

        // response
        return ResponseEntity.ok(new ResponseDTO("Joined chatting room successfully"));
    }

    /**
     * 전체 채팅방 목록을 조회합니다.
     *
     * @param token JWT 토큰
     * @return 전체 채팅방 목록
     */
    public ResponseEntity<List<ChatRoomListResponseDTO>> getAllChatRooms(String token) {
        logger.info("Attempting to retrieve all chatting rooms");

        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }

        // 모든 채팅방 조회
        List<ChatRoom> chatRooms = chatRoomRepository.findAll();
        logger.info("Retrieved {} chatting rooms", chatRooms.size());

        // response
        List<ChatRoomListResponseDTO> responseDTOs = chatRooms.stream()
                .map(chatRoom -> {
                    // 채팅방에 참여한 인원 수 계산
                    int currentMembers = chatRoomMemberRepository.countByChatRoom(chatRoom);
                    logger.debug("Chatting room ID: {} has {} current members", chatRoom.getId(), currentMembers);

                    return new ChatRoomListResponseDTO(
                            chatRoom.getId(),
                            chatRoom.getTitle(),
                            chatRoom.getMaxMembers(),
                            chatRoom.isPrivate(),
                            currentMembers
                    );
                })
                .collect(Collectors.toList());

        logger.info("Returning response with {} chatting rooms", responseDTOs.size());
        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * 사용자가 참여한 채팅방 목록을 조회합니다.
     *
     * @param token JWT 토큰
     * @return 사용자가 참여한 채팅방 목록
     */
    public ResponseEntity<List<ChatRoomListResponseDTO>> getMyChatRooms(String token) {
        logger.info("Attempting to retrieve chatting rooms for user with token");

        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getUsername());

        // 사용자가 참여한 채팅방 목록 조회
        List<ChatRoom> chatRooms = chatRoomMemberRepository.findChatRoomsByUser(user);
        logger.info("User {} is a member of {} chatting rooms", user.getUsername(), chatRooms.size());

        // response
        List<ChatRoomListResponseDTO> responseDTOs = chatRooms.stream()
                .map(chatRoom -> {
                    // 채팅방에 참여한 인원 수 계산
                    int currentMembers = chatRoomMemberRepository.countByChatRoom(chatRoom);
                    logger.debug("Chatting room ID: {} has {} current members", chatRoom.getId(), currentMembers);

                    return new ChatRoomListResponseDTO(
                            chatRoom.getId(),
                            chatRoom.getTitle(),
                            chatRoom.getMaxMembers(),
                            chatRoom.isPrivate(),
                            currentMembers
                    );
                })
                .collect(Collectors.toList());

        logger.info("Returning response with {} chatting rooms for user {}", responseDTOs.size(), user.getUsername());
        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * 사용자가 특정 채팅방의 멤버 목록을 조회합니다.
     *
     * @param token      JWT 토큰
     * @param chatroomId 조회하려는 채팅방의 ID
     * @return 채팅방 멤버 목록 (멤버 ID, 멤버 이름, 가입 일자)
     */
    public ResponseEntity<List<ChatRoomMemberResponseDTO>> getChatRoomMembers(String token, Long chatroomId) {
        logger.info("Attempting to retrieve members for chatting room ID: {}", chatroomId);

        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 조회
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for username: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
        User user = userOpt.get();
        logger.info("User found: {}", user.getUsername());

        // 채팅방 조회
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(chatroomId);
        if (chatRoomOpt.isEmpty()) {
            logger.error("Chatting room not found for ID: {}", chatroomId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
        ChatRoom chatRoom = chatRoomOpt.get();
        logger.info("Chatting room found: {}", chatRoom.getId());

        // 사용자가 채팅방의 멤버인지 확인
        boolean isMember = chatRoomMemberRepository.existsByChatRoomAndUser(chatRoom, user);
        if (!isMember) {
            logger.warn("User {} is not a member of chatting room {}", user.getUsername(), chatRoom.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
        }
        logger.info("User {} is a member of chatting room {}", user.getUsername(), chatRoom.getId());

        // response
        List<ChatRoomMemberResponseDTO> responseDTOs = chatRoomMemberRepository.findByChatRoom(chatRoomOpt.get()).stream()
                .map(member -> new ChatRoomMemberResponseDTO(
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getJoinedAt()
                ))
                .collect(Collectors.toList());

        logger.info("Returning response with {} members for chatting room {}", responseDTOs.size(), chatRoom.getId());
        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * 사용자가 특정 채팅방에서 나가거나 삭제합니다.
     *
     * @param token      JWT 토큰
     * @param chatroomId 나가거나 삭제할 채팅방의 ID
     * @return 상태 코드 (성공적으로 나갔거나 삭제된 경우 204, 또는 조건에 따라 403)
     */
    @Transactional
    public ResponseEntity<ResponseDTO> leaveOrDeleteChatRoom(String token, Long chatroomId) {
        logger.info("Attempting to leave or delete chatting room ID: {}", chatroomId);

        // JWT에서 사용자 이름 추출
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

        // 채팅방 조회
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(chatroomId);
        if (chatRoomOpt.isEmpty()) {
            logger.error("Chatting room not found for ID: {}", chatroomId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDTO("Chatting room not found"));
        }
        ChatRoom chatRoom = chatRoomOpt.get();
        logger.info("Chatting room found: {}", chatRoom.getId());

        // 생성자인지 확인
        boolean isCreator = chatRoom.getCreator().equals(user);
        if (isCreator) { // 생성자라면
            int memberCount = chatRoomMemberRepository.countByChatRoom(chatRoom);
            logger.debug("Chatting room ID: {} has {} members", chatRoom.getId(), memberCount);

            if (memberCount == 1) { // 생성자만 남음
                chatRoomRepository.delete(chatRoom);
                logger.info("Chatting room ID: {} deleted successfully by creator {}", chatRoom.getId(), user.getUsername());
                return ResponseEntity.ok(new ResponseDTO("Chatting room deleted successfully"));
            } else { // 다른 회원도 남음
                logger.warn("Cannot delete chat room if other members remain. Chatting room ID: {}", chatRoom.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ResponseDTO("Cannot delete chat room if other members remain"));
            }
        } else {  // 생성자가 아니라면
            chatRoomMemberRepository.deleteByChatRoomAndUser(chatRoom, user);
            logger.info("User {} left the chatting room ID: {}", user.getUsername(), chatRoom.getId());
            return ResponseEntity.ok(new ResponseDTO("Chatting room left successfully"));
        }
    }
}
