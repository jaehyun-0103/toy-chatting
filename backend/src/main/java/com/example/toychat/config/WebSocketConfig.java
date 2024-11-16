package com.example.toychat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커를 구성하는 메소드
     * @param config MessageBrokerRegistry 객체를 사용하여 메시지 브로커를 설정
     * 이 메소드는 클라이언트가 구독할 수 있는 목적지 prefix를 설정하고,
     * 서버에서 메시지를 전송할 수 있는 간단한 메시지 브로커를 활성화합니다.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * STOMP 엔드포인트를 등록하는 메소드
     * @param registry StompEndpointRegistry 객체를 사용하여 엔드포인트를 설정
     * 이 메소드는 클라이언트가 WebSocket 연결을 설정할 수 있는 엔드포인트를 등록합니다.
     * "/ws" 엔드포인트로 연결이 가능하며, CORS 설정을 통해 외부 도메인에서의 접근을 허용합니다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*");
    }
}
