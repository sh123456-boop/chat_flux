package com.ktb.community.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class KafkaPubSubService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaPubSubService(KafkaTemplate<String, String> kafkaTemplate,
                              SessionRegistry sessionRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    public Mono<Void> publish(String topic, String message) {
        return Mono.fromFuture(kafkaTemplate.send(topic, message)).then();
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
