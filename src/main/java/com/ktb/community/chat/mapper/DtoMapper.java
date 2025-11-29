package com.ktb.community.chat.mapper;

import com.ktb.community.chat.dto.ChatMessageDto;
import com.ktb.community.chat.dto.ChatMessagePubSubDto;
import com.ktb.community.chat.dto.ChatMessageReqDto;
import com.ktb.community.entity.User;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.exception.ErrorCode;
import com.ktb.community.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DtoMapper {
    private final UserRepository userRepository;

    public DtoMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ChatMessageDto toDto(ChatMessageReqDto chatMessageReqDto) {
        User user = userRepository.findById(chatMessageReqDto.getSenderId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        ChatMessageDto chatMessageDto = ChatMessageDto.builder()
                .message(chatMessageReqDto.getMessage())
                .roomId(chatMessageReqDto.getRoomId())
                .senderId(chatMessageReqDto.getSenderId())
                .createdAt(Instant.now())
                .nickName(user.getNickname())
                .build();
        return chatMessageDto;
    }

    public ChatMessagePubSubDto toPubSubDto(ChatMessageReqDto chatMessageReqDto) {

        User user = userRepository.findById(chatMessageReqDto.getSenderId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        ChatMessagePubSubDto chatMessagePubSubDto = ChatMessagePubSubDto.builder()
                .message(chatMessageReqDto.getMessage())
                .roomId(chatMessageReqDto.getRoomId())
                .senderId(chatMessageReqDto.getSenderId())
                .nickName(user.getNickname())
                .build();
        return chatMessagePubSubDto;

    }
}
