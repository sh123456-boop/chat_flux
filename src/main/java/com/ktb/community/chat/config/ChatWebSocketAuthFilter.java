package com.ktb.community.chat.config;

import com.ktb.community.chat.service.ChatServiceImpl;
import com.ktb.community.util.JWTUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class ChatWebSocketAuthFilter implements WebFilter {

    private static final String CHAT_CONNECT_PATH = "/v1/chat/connect";
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketAuthFilter.class);

    private final JWTUtil jwtUtil;
    private final ChatServiceImpl chatService;

    public ChatWebSocketAuthFilter(JWTUtil jwtUtil, ChatServiceImpl chatService) {
        this.jwtUtil = jwtUtil;
        this.chatService = chatService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        // 쿼리 파라미터 포함한 변형 경로도 허용 (/v1/chat/connect, /v1/chat/connect/, etc)
        if (!path.startsWith(CHAT_CONNECT_PATH)) {
            return chain.filter(exchange);
        }
        log.debug("WebSocket auth filter hit: {}", path);

        // WebSocket 브라우저는 커스텀 헤더를 보낼 수 없으므로 쿼리파라미터(access, roomId)만 확인한다.
        String accessToken = exchange.getRequest().getQueryParams().getFirst("access");
        String roomIdParam = exchange.getRequest().getQueryParams().getFirst("roomId");
        if (accessToken == null) {
            return unauthorized(exchange, "missing access token");
        }
        if (roomIdParam == null) {
            return unauthorized(exchange, "missing roomId");
        }

        Long userId;
        Long roomId;

        try {
            // JWT 만료 체크 (동기)
            if (jwtUtil.isExpired(accessToken)) {
                return unauthorized(exchange, "access token expired");
            }

            // JWT 에서 userId 추출 (동기)
            userId = jwtUtil.getID(accessToken);

            // roomId 파싱 (동기)
            roomId = Long.parseLong(roomIdParam);
        } catch (Exception e) {
            // 토큰 파싱 실패, roomId 파싱 실패 등
            return unauthorized(exchange, "invalid token");
        }

        // 여기부터는 DB i/o 포함이므로 Mono 체인 사용
        return chatService.isRoomParticipant(userId, roomId)
                .flatMap(isParticipant -> isParticipant
                        ? chain.filter(exchange)
                        : forbidden(exchange, "access denied for room userId=" + userId + " roomId=" + roomId))
                .onErrorResume(e -> unauthorized(exchange, "invalid token"));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.warn("WebSocket auth unauthorized: {}", message);
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
        var buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        log.warn("WebSocket auth forbidden: {}", message);
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
        var buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
