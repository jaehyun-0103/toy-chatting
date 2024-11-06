package com.example.toychat.controller;

import com.example.toychat.dto.request.ChatRoomCreateRequestDTO;
import com.example.toychat.dto.response.ChatRoomCreateResponseDTO;
import com.example.toychat.dto.response.ChatRoomJoinResponseDTO;
import com.example.toychat.dto.request.ChatRoomJoinRequestDTO;
import com.example.toychat.service.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatrooms")
public class ChatRoomController {

    @Autowired
    private ChatRoomService chatRoomService;

    // 채팅방 생성
    @PostMapping
    public ResponseEntity<ChatRoomCreateResponseDTO> createChatRoom(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody ChatRoomCreateRequestDTO CreateRequestDTO) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return chatRoomService.createChatRoom(token, CreateRequestDTO);
    }

    // 채팅방 참여
    @PostMapping("/join")
    public ResponseEntity<ChatRoomJoinResponseDTO> joinChatRoom(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody ChatRoomJoinRequestDTO joinRequestDTO) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return chatRoomService.joinChatRoom(token, joinRequestDTO);
    }
}
