package com.ktb.community.chat.mapper;

import com.ktb.community.chat.dto.ChatMessageDto;
import com.ktb.community.chat.dto.ChatMessagePubSubDto;
import com.ktb.community.chat.dto.ChatMessageReqDto;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.exception.ErrorCode;
import com.ktb.community.repository.UserRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static reactor.core.scheduler.Schedulers.boundedElastic;

@Component
public class DtoMapper {
    private final UserRepository userRepository;

    public DtoMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<ChatMessagePubSubDto> toPubSubDto(ChatMessageReqDto chatMessageReqDto) {

        return Mono.fromCallable(() -> userRepository.findById(chatMessageReqDto.getSenderId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
                .subscribeOn(boundedElastic())
                .map(user -> ChatMessagePubSubDto.builder()
                        .message(chatMessageReqDto.getMessage())
                        .roomId(chatMessageReqDto.getRoomId())
                        .senderId(chatMessageReqDto.getSenderId())
                        .nickName(user.getNickname())
                        .build());


    }
}
