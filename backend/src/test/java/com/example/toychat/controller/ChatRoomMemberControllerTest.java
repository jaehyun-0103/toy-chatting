package com.example.toychat.controller;

import com.example.toychat.dto.response.ChatRoomMemberResponseDTO;

import com.example.toychat.service.ChatRoomService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ChatRoomMemberControllerTest {

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
    public void testGetChatRoomMembers() throws Exception {
        List<ChatRoomMemberResponseDTO> members = Arrays.asList(
                new ChatRoomMemberResponseDTO(1L, "user1", LocalDateTime.parse("2024-11-01T00:00:00")),
                new ChatRoomMemberResponseDTO(2L, "user2", LocalDateTime.parse("2024-11-01T00:00:00"))
        );

        when(chatRoomService.getChatRoomMembers(any(String.class), any(Long.class)))
                .thenReturn(ResponseEntity.ok(members));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/chatrooms/{chatroom_id}/members", 1L)
                        .header("Authorization", "Bearer some_valid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username", is("user1")))
                .andExpect(jsonPath("$[1].username", is("user2")))
                .andDo(print());
    }

    @Test
    public void testLeaveOrDeleteChatRoom_Success() throws Exception {
        when(chatRoomService.leaveOrDeleteChatRoom(any(String.class), any(Long.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/chatrooms/{chatroom_id}/delete", 1L)
                        .header("Authorization", "Bearer some_valid_token"))
                .andExpect(status().isNoContent())
                .andDo(print());
    }
}
