package com.example.toychat.controller;

import com.example.toychat.dto.request.InviteCodeCreateRequestDTO;
import com.example.toychat.dto.request.InviteCodeJoinRequestDTO;
import com.example.toychat.dto.response.InviteCodeCreateResponseDTO;
import com.example.toychat.dto.response.InviteCodeJoinResponseDTO;

import com.example.toychat.service.InviteCodeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class InviteCodeControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private InviteCodeController inviteCodeController;

    @Mock
    private InviteCodeService inviteCodeService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(inviteCodeController).build();
    }

    @Test
    public void testCreateInviteCode_Success() throws Exception {
        InviteCodeCreateRequestDTO requestDTO = new InviteCodeCreateRequestDTO();
        requestDTO.setChatroomId(1L);

        InviteCodeCreateResponseDTO responseDTO = new InviteCodeCreateResponseDTO("Invite code generated successfully");

        when(inviteCodeService.createInviteCode(any(String.class), any(InviteCodeCreateRequestDTO.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(responseDTO));

        mockMvc.perform(post("/api/invite/create")
                        .header("Authorization", "Bearer some_valid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatroomId\": 1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Invite code generated successfully"));
    }

    @Test
    public void testCreateInviteCode_Failure_UserNotAuthorized() throws Exception {
        InviteCodeCreateRequestDTO requestDTO = new InviteCodeCreateRequestDTO();
        requestDTO.setChatroomId(1L);

        InviteCodeCreateResponseDTO responseDTO = new InviteCodeCreateResponseDTO("You are not authorized to generate an invite code for this chat room.");

        when(inviteCodeService.createInviteCode(any(String.class), any(InviteCodeCreateRequestDTO.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseDTO));

        mockMvc.perform(post("/api/invite/create")
                        .header("Authorization", "Bearer some_valid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatroomId\": 1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not authorized to generate an invite code for this chat room."));
    }

    @Test
    public void testJoinChatRoomUsingInviteCode_Success() throws Exception {
        InviteCodeJoinRequestDTO requestDTO = new InviteCodeJoinRequestDTO();
        requestDTO.setInviteCode("123456");

        InviteCodeJoinResponseDTO responseDTO = new InviteCodeJoinResponseDTO("Joined chat room successfully using invite code");

        when(inviteCodeService.joinChatRoomUsingInviteCode(any(String.class), any(InviteCodeJoinRequestDTO.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK).body(responseDTO));

        mockMvc.perform(post("/api/invite/join")
                        .header("Authorization", "Bearer some_valid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\": \"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Joined chat room successfully using invite code"));
    }

    @Test
    public void testJoinChatRoomUsingInviteCode_Failure_InvalidCode() throws Exception {
        InviteCodeJoinRequestDTO requestDTO = new InviteCodeJoinRequestDTO();
        requestDTO.setInviteCode("000000");

        InviteCodeJoinResponseDTO responseDTO = new InviteCodeJoinResponseDTO("Invalid or expired invite code.");

        when(inviteCodeService.joinChatRoomUsingInviteCode(any(String.class), any(InviteCodeJoinRequestDTO.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDTO));

        mockMvc.perform(post("/api/invite/join")
                        .header("Authorization", "Bearer some_valid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\": \"000000\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invalid or expired invite code."));
    }
}
