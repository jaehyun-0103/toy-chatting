package com.example.toychat.config;

import com.example.toychat.security.JwtFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter; // JWT 필터를 위한 필드

    @Autowired
    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter; // 생성자 주입을 통해 JwtFilter 인스턴스를 초기화
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈을 생성합니다.
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * HTTP 보안 설정을 위한 SecurityFilterChain을 구성합니다.
     * CSRF 보호를 비활성화하고 무상태(Stateless) 세션 정책, 인증 예외 처리 및 엔드포인트 접근 권한을 설정합니다.
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain 객체
     * @throws Exception 보안 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션을 사용하지 않는 무상태(Stateless) 정책 설정
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                (request, response, exception) -> {
                                    response.setStatus(401); // 401 Unauthorized 상태 코드 설정
                                    response.setContentType("application/json"); // 응답 Content-Type 설정
                                    response.setCharacterEncoding("UTF-8"); // 응답 인코딩 설정
                                    response.getWriter().write("{\"message\":\"UNAUTHORIZED\"}"); // 인증 실패 JSON 응답 반환
                                }
                        )
                )
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/register", "/api/login").permitAll() // 등록 및 로그인 API는 인증 없이 접근 허용
                        .requestMatchers("/ws/**").permitAll() // WebSocket 핸드셰이크 경로 접근 허용
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // JWT 인증 필터 등록

        return http.build();
    }
}
