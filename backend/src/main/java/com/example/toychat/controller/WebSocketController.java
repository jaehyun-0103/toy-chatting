package com.example.toychat.controller;

import com.example.toychat.dto.request.MessageSendRequestDTO;
import com.example.toychat.dto.response.MessageResponseDTO;

import com.example.toychat.service.MessageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebSocketController {

    @Autowired
    private MessageService messageService;

    // 메시지 전송 (웹소켓)
    @MessageMapping("/chat.sendMessage/{chatroom_id}")
    @SendTo("/topic/public/{chatroom_id}")
    public MessageResponseDTO sendMessage(
            @DestinationVariable("chatroom_id") Long chatroomId,
            @Payload MessageSendRequestDTO sendRequestDTO,
            @Header("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return messageService.sendMessage(token, chatroomId, sendRequestDTO);
    }
}