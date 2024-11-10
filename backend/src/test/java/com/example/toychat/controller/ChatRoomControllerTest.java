package com.example.toychat.controller;

import com.example.toychat.dto.request.ChatRoomCreateRequestDTO;
import com.example.toychat.dto.response.ChatRoomCreateResponseDTO;
import com.example.toychat.dto.request.ChatRoomJoinRequestDTO;

import com.example.toychat.dto.response.ResponseDTO;
import com.example.toychat.service.ChatRoomService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ChatRoomControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatRoomController chatRoomController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(chatRoomController).build();
    }

    @Test
    void testCreateChatRoom() throws Exception {
        ChatRoomCreateRequestDTO requestDTO = new ChatRoomCreateRequestDTO("Test Room", 10, false);
        ChatRoomCreateResponseDTO responseDTO = new ChatRoomCreateResponseDTO("Chatting room created successfully", 1L, 1L);

        when(chatRoomService.createChatRoom(anyString(), any(ChatRoomCreateRequestDTO.class)))
                .thenReturn(new ResponseEntity<>(responseDTO, HttpStatus.CREATED));

        mockMvc.perform(post("/api/chatrooms")
                        .header("Authorization", "Bearer some_valid_token")
                        .contentType("application/json")
                        .content("{\"title\": \"Test Room\", \"max_members\": 10, \"is_private\": false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chatroom_id").value(1L))
                .andExpect(jsonPath("$.creator_id").value(1L))
                .andExpect(jsonPath("$.message").value("Chatting room created successfully"));

        verify(chatRoomService, times(1)).createChatRoom(anyString(), any(ChatRoomCreateRequestDTO.class));
    }

    @Test
    void testJoinChatRoom() throws Exception {
        ChatRoomJoinRequestDTO requestDTO = new ChatRoomJoinRequestDTO(1L);
        ResponseDTO responseDTO = new ResponseDTO("Joined chat room successfully");

        when(chatRoomService.joinChatRoom(anyString(), any(ChatRoomJoinRequestDTO.class)))
                .thenReturn(new ResponseEntity<>(responseDTO, HttpStatus.OK));

        mockMvc.perform(post("/api/chatrooms/join")
                        .header("Authorization", "Bearer some_valid_token")
                        .contentType("application/json")
                        .content("{\"chatroom_id\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Joined chat room successfully"));

        verify(chatRoomService, times(1)).joinChatRoom(anyString(), any(ChatRoomJoinRequestDTO.class));
    }
}
