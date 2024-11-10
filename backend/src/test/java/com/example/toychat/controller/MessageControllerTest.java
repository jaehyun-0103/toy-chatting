package com.example.toychat.controller;

import com.example.toychat.dto.request.InviteCodeCreateRequestDTO;
import com.example.toychat.dto.request.MessageSendRequestDTO;
import com.example.toychat.dto.request.MessageUpdateRequestDTO;
import com.example.toychat.dto.response.MessageResponseDTO;
import com.example.toychat.dto.response.MessageSendResponseDTO;
import com.example.toychat.dto.response.MessageUpdateResponseDTO;

import com.example.toychat.service.MessageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MessageControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private MessageController messageController;

    @Mock
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(messageController).build();
    }

    @Test
    void sendMessage_ShouldReturn201_WhenMessageIsSentSuccessfully() throws Exception {
        Long chatroomId = 1L;
        MessageSendRequestDTO requestDTO = new MessageSendRequestDTO("Hello, World!");
        MessageSendResponseDTO responseDTO = new MessageSendResponseDTO("Message sent successfully", 1L);

        when(messageService.sendMessage(any(String.class), any(Long.class), any(MessageSendRequestDTO.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(responseDTO));

        mockMvc.perform(post("/api/messages/{chatroom_id}", chatroomId)
                        .header("Authorization", "Bearer some_valid_token")
                        .contentType("application/json")
                        .content("{\"content\":\"Hello, World!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.message").value("Message sent successfully"));

        verify(messageService, times(1)).sendMessage(any(String.class), any(Long.class), eq(requestDTO));
    }

    @Test
    void getMessages_ShouldReturn200_WhenMessagesAreRetrieved() throws Exception {
        Long chatroomId = 1L;
        MessageResponseDTO message1 = new MessageResponseDTO(1L, "user1", "Hello", LocalDateTime.parse("2024-11-09T12:00:00"));
        MessageResponseDTO message2 = new MessageResponseDTO(2L, "user2", "Hi", LocalDateTime.parse("2024-11-09T12:05:00"));
        List<MessageResponseDTO> messageList = Arrays.asList(message1, message2);

        when(messageService.getMessages(any(String.class), any(Long.class)))
                .thenReturn(ResponseEntity.ok(messageList));

        mockMvc.perform(get("/api/messages/{chatroom_id}", chatroomId)
                        .header("Authorization", "Bearer some_valid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message_id").value(1))
                .andExpect(jsonPath("$[1].message_id").value(2));

        verify(messageService, times(1)).getMessages(any(String.class), any(Long.class));
    }

    @Test
    void updateMessage_ShouldReturn200_WhenMessageIsUpdatedSuccessfully() throws Exception {
        Long chatroomId = 1L;
        Long messageId = 1L;
        MessageUpdateRequestDTO requestDTO = new MessageUpdateRequestDTO("Updated Message");
        MessageUpdateResponseDTO responseDTO = new MessageUpdateResponseDTO("Message updated successfully");

        when(messageService.updateMessage(any(String.class), any(Long.class), any(Long.class), any(MessageUpdateRequestDTO.class)))
                .thenReturn(ResponseEntity.ok(responseDTO));

        mockMvc.perform(put("/api/messages/{chatroom_id}/{message_id}", chatroomId, messageId)
                        .header("Authorization", "Bearer some_valid_token")
                        .contentType("application/json")
                        .content("{\"content\":\"Updated Message\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Message updated successfully"));

        verify(messageService, times(1)).updateMessage(any(String.class), any(Long.class), any(Long.class), any(MessageUpdateRequestDTO.class));
    }

    @Test
    void sendMessage_ShouldReturn400_WhenMessageSendFails() throws Exception {
        Long chatroomId = 1L;
        MessageSendRequestDTO requestDTO = new MessageSendRequestDTO("Hello, World!");

        when(messageService.sendMessage(any(String.class), eq(chatroomId), eq(requestDTO)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User not authorized"));

        mockMvc.perform(post("/api/messages/{chatroom_id}", chatroomId)
                        .header("Authorization", "Bearer some_valid_token")
                        .contentType("application/json")
                        .content("{\"content\":\"Hello, World!\"}"))
                .andExpect(status().isForbidden());

        verify(messageService, times(1)).sendMessage(any(String.class), any(Long.class), any(MessageSendRequestDTO.class));
    }
}
