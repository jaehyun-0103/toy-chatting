package com.example.toychat.service;

import com.example.toychat.dto.AuthDTO;
import com.example.toychat.entity.User;
import com.example.toychat.repository.UserRepository;
import com.example.toychat.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.Optional;

@Service
@Validated
public class AuthService {

    @Autowired
    private UserRepository userRepository; // 사용자 리포지토리 주입

    @Autowired
    private JwtUtil jwtUtil; // JWT 유틸리티 주입

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // 비밀번호 인코더 생성

    public ResponseEntity<?> register(AuthDTO authDTO) {
        // 사용자 이름 또는 이메일이 이미 존재하는지 확인
        if (userRepository.findByUsername(authDTO.getUsername()).isPresent() ||
                userRepository.findByEmail(authDTO.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Username or Email already exists" // 이미 존재하는 경우 오류 메시지
            ));
        }

        User user = new User(); // 새 사용자 객체 생성
        user.setUsername(authDTO.getUsername()); // 사용자 이름 설정
        user.setPasswordHash(passwordEncoder.encode(authDTO.getPassword())); // 비밀번호 해시 설정
        user.setEmail(authDTO.getEmail()); // 이메일 설정

        userRepository.save(user); // 사용자 정보 저장

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User created successfully", // 성공 메시지
                "user_id", user.getId() // 생성된 사용자 ID 반환
        ));
    }

    /**
     * 사용자를 로그인하고 JWT를 반환합니다.
     * @param authDTO 로그인할 사용자 정보 데이터 전송 객체
     * @return 로그인 결과에 대한 응답
     */
    public ResponseEntity<?> login(AuthDTO authDTO) {
        // 이메일로 사용자 찾기
        Optional<User> userOpt = userRepository.findByEmail(authDTO.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password")); // 사용자 없음
        }

        User user = userOpt.get(); // 사용자 객체 가져오기

        // 비밀번호 검증
        if (!passwordEncoder.matches(authDTO.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password")); // 비밀번호 불일치
        }

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user.getUsername());

        // 성공 응답 반환
        return ResponseEntity.ok(Map.of("token", token)); // JWT 토큰 반환
    }

    /**
     * 사용자 이름으로 사용자를 로드합니다.
     * @param username 사용자 이름
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자 정보를 찾을 수 없는 경우
     */
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username)); // 사용자 찾기

        return org.springframework.security.core.userdetails.User.builder() // UserDetails 객체 생성
                .username(user.getUsername()) // 사용자 이름 설정
                .password(user.getPasswordHash()) // 비밀번호 해시 설정
                .authorities("USER") // 권한 설정
                .build(); // UserDetails 객체 반환
    }

    /**
     * JWT 인증을 통해 사용자 탈퇴를 처리합니다.
     * @param token JWT 토큰
     * @return 사용자 탈퇴 결과 응답
     */
    public ResponseEntity<?> deleteUser(String token) {
        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        User user = userOpt.get(); // 사용자 객체 가져오기
        userRepository.delete(user); // 사용자 삭제

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 No Content 응답
    }
}