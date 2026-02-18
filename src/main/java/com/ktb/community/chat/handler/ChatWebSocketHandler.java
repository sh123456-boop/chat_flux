package com.ktb.community.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.community.chat.dto.ChatMessageReqDto;
import com.ktb.community.chat.mapper.DtoMapper;
import com.ktb.community.chat.service.ChatServiceImpl;
import com.ktb.community.chat.service.KafkaPubSubService;
import com.ktb.community.chat.service.RedisPubSubService;
import com.ktb.community.chat.service.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebFlux WebSocket 핸들러: subscribe/unsubscribe/chat 메시지를 처리한다.
 */
@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private final SessionRegistry sessionRegistry;
    private final KafkaPubSubService kafkaPubSubService;
    private final ChatServiceImpl chatService;
    private final DtoMapper dtoMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatWebSocketHandler(SessionRegistry sessionRegistry,
                                KafkaPubSubService kafkaPubSubService,
                                ChatServiceImpl chatService,
                                DtoMapper dtoMapper) {
        this.sessionRegistry = sessionRegistry;
        this.kafkaPubSubService = kafkaPubSubService;
        this.chatService = chatService;
        this.dtoMapper = dtoMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 세션 등록 및 outbound flux 준비
        var connection = sessionRegistry.registerSession(session.getId());
        // 세션 등록/송신 스트림(outbound)
        Mono<Void> outbound = session.send(
                connection.getSink().asFlux().map(session::textMessage)
                        .doFinally(signalType -> sessionRegistry.removeSession(session.getId()))
        );

        // 수신 스트림(inbound)
        Mono<Void> inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> handleInbound(session.getId(), payload))
                .doFinally(signalType -> sessionRegistry.removeSession(session.getId()))
                .then();


        // WebSocket 연결 종료 시점을 정의하는 코드
        return Mono.firstWithSignal(outbound, inbound);
    }

    private Mono<Void> handleInbound(String sessionId, String payload) {
        try {
            Map<?, ?> map = objectMapper.readValue(payload, Map.class);
            String type = (String) map.get("type");
            Long roomId = asLong(map.get("roomId"));

            if ("subscribe".equalsIgnoreCase(type) && roomId != null) {
                sessionRegistry.subscribe(sessionId, roomId);
            } else if ("unsubscribe".equalsIgnoreCase(type) && roomId != null) {
                sessionRegistry.unsubscribe(sessionId, roomId);
            } else if ("chat".equalsIgnoreCase(type)) {
                ChatMessageReqDto req = ChatMessageReqDto.builder()
                        .roomId(roomId)
                        .senderId(asLong(map.get("senderId")))
                        .message((String) map.get("message"))
                        .build();

                if (req.getRoomId() == null || req.getSenderId() == null || req.getMessage() == null) {
                    return Mono.empty();
                }

                // 메시지 저장 -> pubsub DTO 변환 -> JSON 직렬화 -> Kafka publish
                return chatService.saveMessage(req.getRoomId(), req)
                        .then(dtoMapper.toPubSubDto(req))
                        .flatMap(dto -> Mono.fromCallable(() -> objectMapper.writeValueAsString(dto)))
//                        .flatMap(message -> redisPubSubService.publish("chat", message).then());
                        .flatMap(message -> kafkaPubSubService.publish("chat", message).then());
            }
        } catch (Exception e) {
            // ignore malformed payload
        }
        return Mono.empty();
    }

    private Long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return value != null ? Long.parseLong(value.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
