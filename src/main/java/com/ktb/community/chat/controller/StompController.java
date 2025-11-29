package com.ktb.community.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.community.chat.dto.ChatMessageDto;
import com.ktb.community.chat.dto.ChatMessagePubSubDto;
import com.ktb.community.chat.dto.ChatMessageReqDto;
import com.ktb.community.chat.mapper.DtoMapper;
import com.ktb.community.chat.service.ChatServiceImpl;
import com.ktb.community.chat.service.RedisPubSubService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

@Controller
@Transactional
public class StompController {

    private final SimpMessageSendingOperations messageTemplate;
    private final ChatServiceImpl chatServiceImpl;
    private final DtoMapper dtoMapper;
    private final RedisPubSubService pubSubService;

    public StompController(SimpMessageSendingOperations messageTemplate,
                           ChatServiceImpl chatServiceImpl,
                           DtoMapper dtoMapper,
                           RedisPubSubService pubSubService) {
        this.messageTemplate = messageTemplate;
        this.chatServiceImpl = chatServiceImpl;
        this.dtoMapper = dtoMapper;
        this.pubSubService = pubSubService;
    }

    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageReqDto chatMessageReqDto) throws JsonProcessingException {
//        /publish/roomId 형태로 오면 해당 room에 메세지 저장
        chatServiceImpl.saveMessage(roomId, chatMessageReqDto);
        chatMessageReqDto.setRoomId(roomId);
        ChatMessagePubSubDto chatMessagePubSubDto = dtoMapper.toPubSubDto(chatMessageReqDto);
//        messageTemplate.convertAndSend("/v1/topic/"+roomId, chatMessageDto);

        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(chatMessagePubSubDto);
        pubSubService.publish("chat", message).subscribe();
    }
}
