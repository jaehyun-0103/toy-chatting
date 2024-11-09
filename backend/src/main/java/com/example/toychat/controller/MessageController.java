package com.example.toychat.controller;

import com.example.toychat.dto.request.MessageUpdateRequestDTO;
import com.example.toychat.dto.request.MessageSendRequestDTO;
import com.example.toychat.dto.response.MessageResponseDTO;
import com.example.toychat.dto.response.MessageSendResponseDTO;
import com.example.toychat.dto.response.MessageUpdateResponseDTO;

import com.example.toychat.service.MessageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // 메시지 전송
    @PostMapping("/{chatroom_id}")
    public ResponseEntity<MessageSendResponseDTO> sendMessage(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("chatroom_id") Long chatroomId,
            @RequestBody MessageSendRequestDTO requestDTO) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return messageService.sendMessage(token, chatroomId, requestDTO);
    }

    // 채팅방 메시지 조회
    @GetMapping("/{chatroom_id}")
    public ResponseEntity<List<MessageResponseDTO>> getMessages(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("chatroom_id") Long chatroomId) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return messageService.getMessages(token, chatroomId);
    }

    // 메시지 수정
    @PutMapping("/{chatroom_id}/{message_id}")
    public ResponseEntity<MessageUpdateResponseDTO> updateMessage(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("chatroom_id") Long chatroomId,
            @PathVariable("message_id") Long messageId,
            @RequestBody MessageUpdateRequestDTO requestDTO) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return messageService.updateMessage(token, chatroomId, messageId, requestDTO);
    }
}
