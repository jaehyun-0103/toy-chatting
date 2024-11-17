package com.example.toychat.controller;

import com.example.toychat.dto.request.MessageUpdateRequestDTO;
import com.example.toychat.dto.response.MessageResponseDTO;

import com.example.toychat.dto.response.ResponseDTO;
import com.example.toychat.service.MessageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
    void testGetMessages() throws Exception {
        Long chatroomId = 1L;
        MessageResponseDTO message1 = new MessageResponseDTO(1L, "user1", 1L,"Hello", LocalDateTime.parse("2024-11-09T12:00:00"));
        MessageResponseDTO message2 = new MessageResponseDTO(2L, "user2", 2L,"Hi", LocalDateTime.parse("2024-11-09T12:05:00"));
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
    void testUpdateMessage() throws Exception {
        Long chatroomId = 1L;
        Long messageId = 1L;
        MessageUpdateRequestDTO requestDTO = new MessageUpdateRequestDTO("Updated Message");
        ResponseDTO responseDTO = new ResponseDTO("Message updated successfully");

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
}
