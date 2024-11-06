package com.example.toychat.service;

import com.example.toychat.dto.request.ChatRoomJoinRequestDTO;
import com.example.toychat.dto.response.ChatRoomCreateResponseDTO;
import com.example.toychat.dto.response.ChatRoomJoinResponseDTO;
import com.example.toychat.dto.request.ChatRoomCreateRequestDTO;
import com.example.toychat.dto.response.ChatRoomListResponseDTO;

import com.example.toychat.entity.ChatRoom;
import com.example.toychat.entity.ChatRoomMember;
import com.example.toychat.entity.User;

import com.example.toychat.repository.ChatRoomMemberRepository;
import com.example.toychat.repository.ChatRoomRepository;
import com.example.toychat.repository.UserRepository;

import com.example.toychat.security.JwtUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatRoomService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 채팅방을 생성합니다.
     * @param token JWT 토큰
     * @param requestDTO 채팅방 생성 요청 데이터
     * @return 채팅방 생성 결과
     */
    public ResponseEntity<ChatRoomCreateResponseDTO> createChatRoom(String token, ChatRoomCreateRequestDTO requestDTO) {
        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        User user = userOpt.get();

        // 채팅방 생성
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setTitle(requestDTO.getTitle());
        chatRoom.setMaxMembers(requestDTO.getMaxMembers());
        chatRoom.setPrivate(requestDTO.isPrivate());
        chatRoom.setCreator(user);

        // 채팅방 저장
        chatRoomRepository.save(chatRoom);

        // 생성자를 채팅방의 첫 참여자로 추가
        ChatRoomMember chatRoomMember = new ChatRoomMember();
        chatRoomMember.setChatRoom(chatRoom);
        chatRoomMember.setUser(user);
        chatRoomMemberRepository.save(chatRoomMember);

        // ChatRoomCreateResponseDTO로 변환하여 응답
        ChatRoomCreateResponseDTO responseDTO = new ChatRoomCreateResponseDTO(
                chatRoom.getId(),
                user.getId(),
                "Chat room created successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    /**
     * 채팅방에 참여합니다.
     * @param token JWT 토큰
     * @param joinRequestDTO 채팅방 참여 요청 데이터
     * @return 채팅방 참여 결과
     */
    public ResponseEntity<ChatRoomJoinResponseDTO> joinChatRoom(String token, ChatRoomJoinRequestDTO joinRequestDTO) {
        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ChatRoomJoinResponseDTO("User not found"));
        }
        User user = userOpt.get();

        // 채팅방 찾기
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(joinRequestDTO.getChatroomId());
        if (chatRoomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ChatRoomJoinResponseDTO("Chat room not found"));
        }
        ChatRoom chatRoom = chatRoomOpt.get();

        // 채팅방에 이미 참여한 사용자인지 확인
        Optional<ChatRoomMember> existingMember = chatRoomMemberRepository.findByChatRoomAndUser(chatRoom, user);
        if (existingMember.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ChatRoomJoinResponseDTO("User is already a member of this chat room"));
        }

        // 채팅방 최대 인원 확인
        if (chatRoom.getMembers().size() >= chatRoom.getMaxMembers()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ChatRoomJoinResponseDTO("Chat room is full"));
        }

        // 채팅방에 사용자 추가
        ChatRoomMember chatRoomMember = new ChatRoomMember();
        chatRoomMember.setChatRoom(chatRoom);
        chatRoomMember.setUser(user);
        chatRoomMemberRepository.save(chatRoomMember);

        // 참여 성공 응답
        return ResponseEntity.ok(new ChatRoomJoinResponseDTO("Joined chat room successfully"));
    }

    /**
     * 전체 채팅방 목록을 조회합니다.
     * @param token JWT 토큰
     * @return 전체 채팅방 목록
     */
    public ResponseEntity<List<ChatRoomListResponseDTO>> getAllChatRooms(String token) {
        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(null);
        }

        // 모든 채팅방 조회
        List<ChatRoom> chatRooms = chatRoomRepository.findAll();

        // 채팅방 정보와 참여 인원 수를 DTO로 변환
        List<ChatRoomListResponseDTO> responseDTOs = chatRooms.stream()
                .map(chatRoom -> {
                    // 채팅방에 참여한 인원 수 계산
                    int currentMembers = chatRoomMemberRepository.countByChatRoom(chatRoom);
                    return new ChatRoomListResponseDTO(
                            chatRoom.getId(),
                            chatRoom.getTitle(),
                            chatRoom.getMaxMembers(),
                            chatRoom.isPrivate(),
                            currentMembers
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * 사용자가 참여한 채팅방 목록을 조회합니다.
     * @param token JWT 토큰
     * @return 사용자가 참여한 채팅방 목록
     */
    public ResponseEntity<List<ChatRoomListResponseDTO>> getMyChatRooms(String token) {
        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(null);
        }
        User user = userOpt.get();

        // 사용자가 참여한 채팅방 목록 조회
        List<ChatRoom> chatRooms = chatRoomMemberRepository.findChatRoomsByUser(user);

        // 채팅방 정보와 참여 인원 수를 DTO로 변환
        List<ChatRoomListResponseDTO> responseDTOs = chatRooms.stream()
                .map(chatRoom -> {
                    // 채팅방에 참여한 인원 수 계산
                    int currentMembers = chatRoomMemberRepository.countByChatRoom(chatRoom);
                    return new ChatRoomListResponseDTO(
                            chatRoom.getId(),
                            chatRoom.getTitle(),
                            chatRoom.getMaxMembers(),
                            chatRoom.isPrivate(),
                            currentMembers
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDTOs);
    }
}
