package com.example.toychat.security;

import com.example.toychat.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; // JWT 유틸리티를 주입
    private final UserRepository userRepository; // 사용자 리포지토리를 주입

    /**
     * JwtFilter 생성자입니다.
     *
     * @param jwtUtil        JWT 유틸리티
     * @param userRepository 사용자 리포지토리
     */
    public JwtFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    /**
     * 요청을 필터링하여 JWT의 유효성을 검사하고 인증 정보를 설정합니다.
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException      IO 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath(); // 요청 서블릿 경로 추출

        // WebSocket 핸드셰이크 요청이면 필터링을 거치지 않도록 설정
        if (path.equals("/ws") || path.startsWith("/ws/")) {
            filterChain.doFilter(request, response);
            return; // WebSocket 연결은 JWT 필터를 거치지 않음
        }

        String header = request.getHeader("Authorization"); // Authorization 헤더 가져오기

        // Authorization 헤더가 없는 경우 다음 필터로 진행
        if (header == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더가 Bearer로 시작하는지 확인
        if (!header.startsWith("Bearer ")) {
            unauthorized(response); // 인증 실패 응답 전송
            return;
        }

        try {
            Long userId = jwtUtil.extractUserId(header.substring(7)); // JWT에서 사용자 ID 추출

            // 사용자 ID 존재 여부 확인
            if (!userRepository.existsById(userId)) {
                unauthorized(response); // 존재하지 않는 사용자인 경우 인증 실패 응답 전송
                return;
            }

            // 인증 토큰 객체 생성
            var authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null,List.of(new SimpleGrantedAuthority("USER")));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // 추가 인증 정보 설정

            SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContext에 인증 정보 저장
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext(); // SecurityContext 초기화
            unauthorized(response); // 토큰 검증 실패 시 인증 실패 응답 전송
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 인증 실패 시 401 Unauthorized JSON 응답을 전송합니다.
     *
     * @param response HTTP 응답
     * @throws IOException IO 예외
     */
    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 응답 상태 코드 401 설정
        response.setContentType("application/json"); // 응답 Content-Type 설정
        response.setCharacterEncoding("UTF-8"); // 응답 인코딩 UTF-8 설정
        response.getWriter().write("{\"message\":\"UNAUTHORIZED\"}"); // 인증 실패 메시지 출력
    }
}