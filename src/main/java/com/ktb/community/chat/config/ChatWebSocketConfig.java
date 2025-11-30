package com.ktb.community.chat.config;

import com.ktb.community.chat.handler.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFlux
public class ChatWebSocketConfig {

    @Value("${spring.route.front}")
    private String front;

    /**
     * 매핑: /v1/chat/connect -> ChatWebSocketHandler
     */
    @Bean
    public HandlerMapping chatWebSocketMapping(ChatWebSocketHandler handler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/v1/chat/connect", handler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(-1);
        mapping.setUrlMap(map);

        // ✅ /v1/chat/connect 에 대한 CORS 설정
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(
                front,                      // application.yml 에서 읽어온 프론트 주소
                "http://localhost:8080",
                "http://127.0.0.1:8000"
        ));
        cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setAllowCredentials(true);

        Map<String, CorsConfiguration> corsMap = new HashMap<>();
        corsMap.put("/v1/chat/connect", cors);

        mapping.setCorsConfigurations(corsMap);

        return mapping;
    }

    @Bean
    public WebSocketService webSocketService() {
        ReactorNettyRequestUpgradeStrategy strategy = new ReactorNettyRequestUpgradeStrategy();
        HandshakeWebSocketService service = new HandshakeWebSocketService(strategy);
        return service;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter(WebSocketService webSocketService) {
        return new WebSocketHandlerAdapter(webSocketService);
    }
}
