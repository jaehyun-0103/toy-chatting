package com.example.toychat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey; // JWT 서명용 SecretKey
    private final JwtParser parser; // JWT 파서 객체
    private final long expirationMillis; // JWT 만료 시간 (밀리초)

    /**
     * JwtUtil 생성자입니다.
     *
     * @param encodedSecret    Base64로 인코딩된 비밀 키 문자열
     * @param expirationMillis JWT 만료 시간 (밀리초)
     */
    public JwtUtil(
            @Value("${JWT_SECRET}") String encodedSecret,
            @Value("${app.jwt.expiration-ms:36000000}")
            long expirationMillis
    ) {
        byte[] keyBytes; // 디코딩된 비밀 키 바이트 배열

        try {
            keyBytes = Base64.getDecoder().decode(encodedSecret); // Base64 비밀 키 디코딩
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET must be valid Base64");
        }

        // 비밀 키 최소 길이 검증
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 decoded bytes");
        }

        // 만료 시간 양수 여부 검증
        if (expirationMillis <= 0) {
            throw new IllegalStateException("app.jwt.expiration-ms must be positive");
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes); // HMAC-SHA SecretKey 생성
        this.expirationMillis = expirationMillis; // 만료 시간 설정
        this.parser = Jwts.parserBuilder().setSigningKey(secretKey).build(); // JWT 파서 빌드
    }

    /**
     * 사용자 ID를 기반으로 JWT를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 생성된 JWT 문자열
     */
    public String generateToken(Long userId) {
        // 사용자 ID 유효성 검사
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        long now = System.currentTimeMillis(); // 현재 시간 (밀리초)

        return Jwts.builder()
                .setSubject(userId.toString()) // 주제(사용자 ID) 설정
                .setIssuedAt(new Date(now)) // 발행 일시 설정
                .setExpiration(new Date(now + expirationMillis)) // 만료 일시 설정
                .signWith(secretKey, SignatureAlgorithm.HS256) // SecretKey 및 HS256 알고리즘으로 서명
                .compact(); // 토큰 문자열로 변환
    }

    /**
     * JWT에서 사용자 ID를 추출합니다.
     *
     * @param token JWT 문자열
     * @return 추출된 사용자 ID
     */
    public Long extractUserId(String token) {
        String subject = parseClaims(token).getSubject(); // 토큰 클레임에서 주제 추출

        // 주제 형식(양의 정수 문자열) 검증
        if (subject == null || !subject.matches("[1-9][0-9]*")) {
            throw new MalformedJwtException("Invalid user ID subject");
        }

        try {
            return Long.valueOf(subject); // Long 타입 사용자 ID로 변환
        } catch (NumberFormatException ex) {
            throw new MalformedJwtException("Invalid user ID subject");
        }
    }

    /**
     * JWT에서 만료 일시를 추출합니다.
     *
     * @param token JWT 문자열
     * @return 추출된 만료 일시
     */
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration(); // 클레임에서 만료 일시 반환
    }

    /**
     * JWT를 검증하고 모든 클레임을 추출합니다.
     *
     * @param token JWT 문자열
     * @return Claims 객체
     */
    private Claims parseClaims(String token) {
        var parsed = parser.parseClaimsJws(token); // JWT 서명 검증 및 파싱

        // 서명 알고리즘이 HS256인지 검증
        if (!SignatureAlgorithm.HS256.getValue().equals(parsed.getHeader().getAlgorithm())) {
            throw new MalformedJwtException("Unsupported JWT algorithm");
        }

        Claims claims = parsed.getBody(); // 클레임 본문 추출
        Date expiration = claims.getExpiration(); // 만료 일시 추출

        // 만료 일시 존재 및 유효성 검증
        if (expiration == null || !expiration.after(new Date())) {
            throw new MalformedJwtException("Invalid JWT expiration");
        }

        return claims; // 검증된 클레임 반환
    }
}