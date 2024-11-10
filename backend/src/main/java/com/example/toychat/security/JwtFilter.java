package com.example.toychat.security;

import com.example.toychat.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil; // JWT 유틸리티를 주입

    @Autowired
    private AuthService authService; // 인증 서비스를 주입

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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization"); // Authorization 헤더 가져오기

        String username = null; // 사용자 이름 변수 초기화
        String jwt = null; // JWT 변수 초기화

        // Authorization 헤더가 존재하고 Bearer로 시작하는지 확인
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // JWT 추출
            username = jwtUtil.extractUsername(jwt); // JWT에서 사용자 이름 추출
        }

        // 사용자 이름이 존재하고, 현재 인증되지 않은 경우
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = authService.loadUserByUsername(username);  // AuthService에서 사용자 로드

            // JWT가 유효한지 검사
            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // 추가 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authenticationToken); // SecurityContext에 인증 정보 저장
            }
        }

        filterChain.doFilter(request, response);
    }
}
