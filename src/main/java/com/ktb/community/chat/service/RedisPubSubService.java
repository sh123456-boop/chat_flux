package com.ktb.community.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.community.chat.dto.ChatMessageDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RedisPubSubService {

    private final ReactiveStringRedisTemplate stringRedisTemplate;
    private final SimpMessageSendingOperations messageTemplate;

    public RedisPubSubService(@Qualifier("chatPubSub") ReactiveStringRedisTemplate stringRedisTemplate,
                              SimpMessageSendingOperations messageTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.messageTemplate = messageTemplate;
    }

    public Mono<Long> publish(String channel, String message) {
        return stringRedisTemplate.convertAndSend(channel, message);
    }

    public Mono<Void> handleMessage(String payload) {
        return Mono.fromRunnable(() -> {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                ChatMessageDto chatMessageDto = objectMapper.readValue(payload, ChatMessageDto.class);
                messageTemplate.convertAndSend("/v1/chat/topic/" + chatMessageDto.getRoomId(), chatMessageDto);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
