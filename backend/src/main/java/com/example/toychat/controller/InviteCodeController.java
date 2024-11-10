package com.example.toychat.controller;

import com.example.toychat.dto.request.InviteCodeCreateRequestDTO;
import com.example.toychat.dto.request.InviteCodeJoinRequestDTO;
import com.example.toychat.dto.response.InviteCodeCreateResponseDTO;
import com.example.toychat.dto.response.ResponseDTO;

import com.example.toychat.service.InviteCodeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invite")
public class InviteCodeController {

    @Autowired
    private InviteCodeService inviteCodeService;

    // 초대 코드 생성
    @PostMapping("/create")
    public ResponseEntity<InviteCodeCreateResponseDTO> createInviteCode(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody InviteCodeCreateRequestDTO requestDTO) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return inviteCodeService.createInviteCode(token, requestDTO);
    }

    // 초대 코드로 채팅방에 참여
    @PostMapping("/join")
    public ResponseEntity<ResponseDTO> joinChatRoomUsingInviteCode(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody InviteCodeJoinRequestDTO joinRequestDTO) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return inviteCodeService.joinByInviteCode(token, joinRequestDTO);
    }
}
