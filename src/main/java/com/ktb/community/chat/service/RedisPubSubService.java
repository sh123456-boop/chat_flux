package com.ktb.community.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.community.chat.dto.ChatMessageDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RedisPubSubService {

    private final ReactiveStringRedisTemplate stringRedisTemplate;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisPubSubService(@Qualifier("chatPubSub") ReactiveStringRedisTemplate stringRedisTemplate,
                              SessionRegistry sessionRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    public Mono<Long> publish(String channel, String message) {
        return stringRedisTemplate.convertAndSend(channel, message);
    }

    public Mono<Void> handleMessage(String payload) {
        return Mono.fromRunnable(() -> {
            try {
                JsonNode node = objectMapper.readTree(payload);
                Long roomId = node.path("roomId").isNumber() ? node.path("roomId").asLong() : null;
                if (roomId != null) {
                    sessionRegistry.broadcast(roomId, payload);
                }
            } catch (Exception e) {
                // ignore malformed payload to avoid crashing listener
            }
        });
    }
}
