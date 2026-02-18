package com.ktb.community.chat.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageListener {

    private final KafkaPubSubService kafkaPubSubService;

    public KafkaMessageListener(KafkaPubSubService kafkaPubSubService) {
        this.kafkaPubSubService = kafkaPubSubService;
    }

    @KafkaListener(
            topics = "${app.chat.topic:chat}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(String payload) {
        kafkaPubSubService.handleMessage(payload).subscribe();
    }
}
