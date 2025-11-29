package com.ktb.community.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.community.chat.dto.ChatMessageReqDto;
import com.ktb.community.chat.mapper.DtoMapper;
import com.ktb.community.chat.service.ChatServiceImpl;
import com.ktb.community.chat.service.RedisPubSubService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class StompController {

    private final ChatServiceImpl chatServiceImpl;
    private final DtoMapper dtoMapper;
    private final RedisPubSubService pubSubService;

    public StompController(ChatServiceImpl chatServiceImpl,
                           DtoMapper dtoMapper,
                           RedisPubSubService pubSubService) {
        this.chatServiceImpl = chatServiceImpl;
        this.dtoMapper = dtoMapper;
        this.pubSubService = pubSubService;
    }

    @MessageMapping("/{roomId}")
    public Mono<Void> sendMessage(@DestinationVariable Long roomId, ChatMessageReqDto chatMessageReqDto) {
        // /publish/roomId 형태로 오면 해당 room에 메세지 저장
        chatMessageReqDto.setRoomId(roomId);
        return chatServiceImpl.saveMessage(roomId, chatMessageReqDto)
                .then(dtoMapper.toPubSubDto(chatMessageReqDto))
                .flatMap(dto -> Mono.fromCallable(() -> new ObjectMapper().writeValueAsString(dto)))
                .flatMap(message -> pubSubService.publish("chat", message))
                .then();
    }
}
