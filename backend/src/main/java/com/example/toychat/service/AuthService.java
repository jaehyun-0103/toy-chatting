package com.example.toychat.service;

import com.example.toychat.dto.AuthDTO;

import com.example.toychat.entity.User;

import com.example.toychat.repository.UserRepository;

import com.example.toychat.security.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil; // JWT 유틸리티 주입

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // 비밀번호 인코더 생성

    /**
     * 사용자 이름으로 사용자를 로드합니다.
     *
     * @param username 사용자 이름
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자 정보를 찾을 수 없는 경우
     */
    public UserDetails loadUserByUsername(String username) {
        logger.info("Attempting to load user by username: {}", username);

        // 사용자 찾기
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        logger.info("User found: {}", username);

        return org.springframework.security.core.userdetails.User.builder() // UserDetails 객체 생성
                .username(user.getUsername()) // 사용자 이름 설정
                .password(user.getPasswordHash()) // 비밀번호 해시 설정
                .authorities("USER") // 권한 설정
                .build(); // UserDetails 객체 반환
    }

    /**
     * 회원 가입 기능을 수행합니다.
     *
     * @param authDTO 사용자 등록 정보가 담긴 DTO
     * @return 회원 가입 결과 응답
     */
    public ResponseEntity<?> register(AuthDTO authDTO) {
        logger.info("Attempting to register user with email: {}", authDTO.getEmail());

        // 사용자 이름이 비어 있는지 확인
        if (!StringUtils.hasText(authDTO.getUsername())) {
            logger.warn("Username is required for registration.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Username is required"));
        }

        // 비밀번호가 비어 있는지 확인
        if (!StringUtils.hasText(authDTO.getPassword())) {
            logger.warn("Password is required for registration.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Password is required"));
        }

        // 이메일이 비어 있는지 확인하고 이메일 형식 검사
        if (!StringUtils.hasText(authDTO.getEmail()) || !authDTO.getEmail().contains("@")) {
            logger.warn("Invalid email format provided.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid email format"));
        }

        // 사용자 이름 또는 이메일이 이미 존재하는지 확인
        if (userRepository.findByUsername(authDTO.getUsername()).isPresent() ||
                userRepository.findByEmail(authDTO.getEmail()).isPresent()) {
            logger.warn("Username or Email already exists: {}", authDTO.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Username or Email already exists"
            ));
        }

        // 사용자 정보 저장
        User user = new User();
        user.setUsername(authDTO.getUsername());
        user.setPasswordHash(passwordEncoder.encode(authDTO.getPassword()));
        user.setEmail(authDTO.getEmail());
        userRepository.save(user);
        logger.info("User created successfully with ID: {}", user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User created successfully",
                "user_id", user.getId()
        ));
    }

    /**
     * 사용자를 로그인하고 JWT를 반환합니다.
     *
     * @param authDTO 로그인할 사용자 정보 데이터 전송 객체
     * @return 로그인 결과에 대한 응답
     */
    public ResponseEntity<?> login(AuthDTO authDTO) {
        logger.info("Attempting to login user with email: {}", authDTO.getEmail());

        // 이메일이 비어 있는지 확인하고 이메일 형식 검사
        if (!StringUtils.hasText(authDTO.getEmail()) || !authDTO.getEmail().contains("@")) {
            logger.warn("Invalid email format provided.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid email format"));
        }

        // 비밀번호가 비어 있는지 확인
        if (!StringUtils.hasText(authDTO.getPassword())) {
            logger.warn("Password is required for login.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Password is required"));
        }

        // 이메일로 사용자 찾기
        Optional<User> userOpt = userRepository.findByEmail(authDTO.getEmail());
        if (userOpt.isEmpty()) {
            logger.error("Invalid email or password for email: {}", authDTO.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email"));
        }
        User user = userOpt.get();

        // 해당 이메일 사용자의 비밀번호 검증
        if (!passwordEncoder.matches(authDTO.getPassword(), user.getPasswordHash())) {
            logger.error("Invalid email or password for email: {}", authDTO.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid password"));
        }

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user.getUsername());
        logger.info("Login successful for email: {}", authDTO.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "Login is successfully",
                "token", token,
                "user_id", user.getId()
        ));
    }

    /**
     * JWT 인증을 통해 사용자 탈퇴를 처리합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 탈퇴 결과 응답
     */
    public ResponseEntity<?> deleteUser(String token) {
        logger.info("Attempting to delete user with token: {}", token);

        // JWT에서 사용자 이름 추출
        String username = jwtUtil.extractUsername(token);
        logger.debug("Extracted username from JWT: {}", username);

        // 사용자 찾기
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.error("User not found for deletion: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User does not exist"));
        }
        User user = userOpt.get();

        // 사용자 참여한 채팅방 확인
        if (!user.getChatRooms().isEmpty()) {
            logger.error("User still participates in chat rooms: {}", username);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "User must leave all chat rooms before deleting account"));
        }

        // 사용자 삭제
        userRepository.delete(user);
        logger.info("User deleted successfully: {}", username);

        return ResponseEntity.ok(Map.of(
                "message", "User deleted successfully"
        ));
    }
}