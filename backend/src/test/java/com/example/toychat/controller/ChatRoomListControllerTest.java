package com.example.toychat.controller;

import com.example.toychat.dto.response.ChatRoomListResponseDTO;
import com.example.toychat.entity.ChatRoom;
import com.example.toychat.service.ChatRoomService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class ChatRoomListControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatRoomController chatRoomController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(chatRoomController).build();
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    public void testGetAllChatRooms() throws Exception {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setId(1L);
        chatRoom.setTitle("Test Chat Room");
        chatRoom.setMaxMembers(10);
        chatRoom.setPrivate(false);

        when(chatRoomService.getAllChatRooms(anyString())).thenReturn(ResponseEntity.ok(
                List.of(new ChatRoomListResponseDTO(1L, "Test Chat Room", 10, false, 2))
        ));

        mockMvc.perform(get("/api/chatrooms")
                        .header("Authorization", "Bearer test_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Chat Room"))
                .andExpect(jsonPath("$[0].max_members").value(10))
                .andExpect(jsonPath("$[0].is_private").value(false))
                .andExpect(jsonPath("$[0].current_members").value(2));

        verify(chatRoomService, times(1)).getAllChatRooms(anyString());
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    public void testGetMyChatRooms() throws Exception {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setId(1L);
        chatRoom.setTitle("My Chat Room");
        chatRoom.setMaxMembers(10);
        chatRoom.setPrivate(false);

        when(chatRoomService.getMyChatRooms(anyString())).thenReturn(ResponseEntity.ok(
                List.of(new ChatRoomListResponseDTO(1L, "My Chat Room", 10, false, 2))
        ));

        mockMvc.perform(get("/api/chatrooms/lists")
                        .header("Authorization", "Bearer test_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("My Chat Room"))
                .andExpect(jsonPath("$[0].max_members").value(10))
                .andExpect(jsonPath("$[0].is_private").value(false))
                .andExpect(jsonPath("$[0].current_members").value(2));

        verify(chatRoomService, times(1)).getMyChatRooms(anyString());
    }
}
