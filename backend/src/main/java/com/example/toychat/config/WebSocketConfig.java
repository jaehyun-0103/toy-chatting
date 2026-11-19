package com.example.toychat.config;

import com.example.toychat.security.StompHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    /**
     * WebSocketConfig 생성자 메소드
     * @param stompHandler STOMP 메시지 인증 및 권한 검증을 처리하는 핸들러
     * 이 메소드는 STOMP 채널 인터셉터로 사용할 StompHandler를 주입받아 초기화합니다.
     */
    public WebSocketConfig(StompHandler stompHandler) {
        this.stompHandler = stompHandler;
    }

    /**
     * 메시지 브로커를 구성하는 메소드
     * @param config MessageBrokerRegistry 객체를 사용하여 메시지 브로커를 설정
     * 이 메소드는 클라이언트가 구독할 수 있는 목적지 prefix를 설정하고,
     * 서버에서 메시지를 전송할 수 있는 간단한 메시지 브로커를 활성화하며 메시지 발행 순서를 보장합니다.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setPreservePublishOrder(true);
    }

    /**
     * 클라이언트 인바운드 채널을 구성하는 메소드
     * @param registration ChannelRegistration 객체를 사용하여 인바운드 채널을 설정
     * 이 메소드는 클라이언트로부터 들어오는 STOMP 메시지를 가로채 인증 및 권한을 검증하는 인터셉터를 등록합니다.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }

    /**
     * 클라이언트 아웃바운드 채널을 구성하는 메소드
     * @param registration ChannelRegistration 객체를 사용하여 아웃바운드 채널을 설정
     * 이 메소드는 클라이언트로 전달되는 아웃바운드 메시지의 세션 유효성과 멤버십을 재검증하는 인터셉터를 등록합니다.
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler.outboundInterceptor());
    }

    /**
     * STOMP 엔드포인트를 등록하는 메소드
     * @param registry StompEndpointRegistry 객체를 사용하여 엔드포인트를 설정
     * 이 메소드는 클라이언트가 WebSocket 연결을 설정할 수 있는 엔드포인트를 등록하고 수신 순서를 보장합니다.
     * "/ws" 엔드포인트로 연결이 가능하며, CORS 설정을 통해 외부 도메인에서의 접근을 허용합니다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.setPreserveReceiveOrder(true);
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*");
    }
}