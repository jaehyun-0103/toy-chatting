package com.example.toychat.controller;

import com.example.toychat.dto.request.ChatRoomCreateRequestDTO;
import com.example.toychat.dto.request.ChatRoomJoinRequestDTO;
import com.example.toychat.dto.response.ChatRoomCreateResponseDTO;
import com.example.toychat.dto.response.ChatRoomListResponseDTO;
import com.example.toychat.dto.response.ChatRoomMemberResponseDTO;
import com.example.toychat.dto.response.ResponseDTO;

import com.example.toychat.service.ChatRoomService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ResponseDTO> joinChatRoom(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody ChatRoomJoinRequestDTO joinRequestDTO) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return chatRoomService.joinChatRoom(token, joinRequestDTO);
    }

    // 전체 채팅방 목록 조회
    @GetMapping
    public ResponseEntity<List<ChatRoomListResponseDTO>> getAllChatRooms(
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return chatRoomService.getAllChatRooms(token);
    }

    // 사용자가 참여한 채팅방 목록 조회
    @GetMapping("/lists")
    public ResponseEntity<List<ChatRoomListResponseDTO>> getUserChatRooms(
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return chatRoomService.getMyChatRooms(token);
    }

    // 채팅방의 멤버 목록을 조회
    @GetMapping("/{chatroom_id}/members")
    public ResponseEntity<List<ChatRoomMemberResponseDTO>> getChatRoomMembers(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("chatroom_id") Long chatroomId) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return chatRoomService.getChatRoomMembers(token, chatroomId);
    }

    // 채팅방 탈퇴 및 삭제
    @DeleteMapping("/{chatroom_id}/delete")
    public ResponseEntity<ResponseDTO> leaveOrDeleteChatRoom(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("chatroom_id") Long chatroomId) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return chatRoomService.leaveOrDeleteChatRoom(token, chatroomId);
    }
}
