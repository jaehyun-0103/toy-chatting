package com.example.toychat.controller;

import com.example.toychat.dto.AuthDTO;

import com.example.toychat.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService; // 인증 서비스에 대한 의존성 주입

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthDTO authDTO) {
        return authService.register(authDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDTO authDTO) {
        return authService.login(authDTO);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7); // "Bearer " 제거
        return authService.deleteUser(token);
    }
}
