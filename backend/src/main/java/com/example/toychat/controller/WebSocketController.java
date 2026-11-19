package com.example.toychat.controller;

import com.example.toychat.dto.request.MessageSendRequestDTO;
import com.example.toychat.dto.response.MessageResponseDTO;
import com.example.toychat.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class WebSocketController {

    private final MessageService messageService;

    // 생성자 주입
    public WebSocketController(MessageService messageService) {
        this.messageService = messageService;
    }

    // 메시지 전송 (웹소켓)
    @MessageMapping("/chat.sendMessage/{chatroom_id}")
    @SendTo("/topic/public/{chatroom_id}")
    public MessageResponseDTO sendMessage(
            @DestinationVariable("chatroom_id") Long chatroomId,
            @Payload MessageSendRequestDTO sendRequestDTO,
            Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName()); // 인증 정보에서 사용자 ID 추출

        return messageService.sendMessage(userId, chatroomId, sendRequestDTO);
    }
}