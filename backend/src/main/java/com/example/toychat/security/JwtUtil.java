package com.example.toychat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256); // 안전한 SecretKey 생성

    /**
     * 사용자 이름을 기반으로 JWT를 생성합니다.
     *
     * @param username 사용자 이름
     * @return 생성된 JWT 문자열
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>(); // JWT에 포함될 클레임 생성
        return createToken(claims, username); // 클레임과 사용자 이름으로 토큰 생성
    }

    /**
     * 주어진 클레임과 주제를 기반으로 JWT를 생성합니다.
     *
     * @param claims  JWT에 포함될 클레임
     * @param subject JWT의 주제 (사용자 이름 등)
     * @return 생성된 JWT 문자열
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims) // 클레임 설정
                .setSubject(subject) // 주제 설정
                .setIssuedAt(new Date(System.currentTimeMillis())) // 발행 일시 설정
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10시간 후 만료 설정
                .signWith(SECRET_KEY) // SecretKey로 서명
                .compact(); // 토큰 문자열로 변환
    }

    /**
     * JWT의 유효성을 검사합니다.
     *
     * @param token    JWT 문자열
     * @param username 사용자 이름
     * @return JWT가 유효한 경우 true, 그렇지 않은 경우 false
     */
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token); // 토큰에서 사용자 이름 추출
        return (extractedUsername.equals(username) && !isTokenExpired(token)); // 사용자 이름 일치 및 만료 여부 확인
    }

    /**
     * JWT에서 사용자 이름을 추출합니다.
     *
     * @param token JWT 문자열
     * @return 추출된 사용자 이름
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject(); // 토큰의 클레임에서 사용자 이름 추출
    }

    /**
     * JWT에서 모든 클레임을 추출합니다.
     *
     * @param token JWT 문자열
     * @return Claims 객체
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY) // SecretKey 설정
                .build()
                .parseClaimsJws(token) // JWT 파싱
                .getBody(); // 클레임 본문 반환
    }

    /**
     * JWT의 만료 여부를 확인합니다.
     *
     * @param token JWT 문자열
     * @return 만료된 경우 true, 그렇지 않은 경우 false
     */
    private Boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date()); // 현재 시간과 만료 시간 비교
    }
}
