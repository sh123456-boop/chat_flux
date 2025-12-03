package com.ktb.community.llm.service;


import com.ktb.community.chat.entity.ChatMessage;
import com.ktb.community.chat.entity.ChatRoom;
import com.ktb.community.chat.repository.ChatMessageRepository;
import com.ktb.community.chat.repository.ChatRoomRepository;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.llm.dto.*;
import com.ktb.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

import static com.ktb.community.exception.ErrorCode.INTERNAL_SERVER_ERROR;
import static com.ktb.community.exception.ErrorCode.MEMBER_NOT_FOUND;
import static com.ktb.community.exception.ErrorCode.ROOM_NOT_FOUND;
import static reactor.core.scheduler.Schedulers.boundedElastic;

@Service
@RequiredArgsConstructor
public class LightLlmService {

    private final WebClient webClient;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${llm.gemini.key}")
    private String geminiApiKey;

    public Mono<UserLlmChatResponseDto> getChat(Long roomId, Long userId) {
        return Mono.fromCallable(() -> {
                    List<String> messages = fetchFormattedMessages(roomId);
                    String requesterNickname = fetchUserNickname(userId);
                    return buildGeminiRequest(messages, requesterNickname);
                })
                .subscribeOn(boundedElastic())
                .flatMap(this::callGemini)
                .map(this::toUserResponse);
    }

    private List<String> fetchFormattedMessages(Long roomId) {
        return Optional.ofNullable(transactionTemplate.execute(status -> {
                    ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                            .orElseThrow(() -> new BusinessException(ROOM_NOT_FOUND));
                    List<ChatMessage> recentMessages = chatMessageRepository.findTop10ByChatRoomOrderByCreatedAtDesc(chatRoom);
                    Collections.reverse(recentMessages); // 가장 오래된 것부터 정렬
                    return recentMessages.stream()
                            .map(this::formatMessageWithSender)
                            .toList();
                }))
                .orElseThrow(() -> new BusinessException(INTERNAL_SERVER_ERROR));
    }

    private String fetchUserNickname(Long userId) {
        return Optional.ofNullable(transactionTemplate.execute(status ->
                        userRepository.findById(userId)
                                .map(user -> Optional.ofNullable(user.getNickname()).orElse("알 수 없는 사용자"))
                                .orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND))
                ))
                .orElseThrow(() -> new BusinessException(INTERNAL_SERVER_ERROR));
    }

    private GeminiGenerateContentRequestDto buildGeminiRequest(List<String> messages, String requesterNickname) {
        List<GeminiContentDto> contents = new ArrayList<>();
        contents.add(GeminiContentDto.user("""
                너는 인증된 사용자 [%s]의 시점에서 대화에 참여하는 한국어 어시스턴트야.
                아래 대화 기록을 보고 [%s]이 이어서 전송할 자연스러운 다음 메시지를 작성해.
                상대방 메시지를 그대로 이어 쓰거나 보낸 사람을 바꾸지 말고, 맥락에 맞는 새 발화를 만들어.
                메시지는 [닉네임]: 내용 형식으로 제공돼.
                """.formatted(requesterNickname, requesterNickname).trim()));

        for (String message : messages) {
            contents.add(GeminiContentDto.user(message));
        }

        return GeminiGenerateContentRequestDto.builder()
                .contents(contents)
                .build();
    }

    private String formatMessageWithSender(ChatMessage chatMessage) {
        String senderName = Optional.ofNullable(chatMessage.getUser())
                .map(user -> Optional.ofNullable(user.getNickname()).orElse("알 수 없는 사용자"))
                .orElse("알 수 없는 사용자");
        return "[" + senderName + "]: " + chatMessage.getContents();
    }

    private Mono<GeminiGenerateContentResponseDto> callGemini(GeminiGenerateContentRequestDto requestDto) {
        return webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey)
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(GeminiGenerateContentResponseDto.class);
    }

    private UserLlmChatResponseDto toUserResponse(GeminiGenerateContentResponseDto responseDto) {
        String answer = Optional.ofNullable(responseDto.getCandidates())
                .orElseGet(List::of)
                .stream()
                .map(candidate -> Optional.ofNullable(candidate.getContent())
                        .map(content -> Optional.ofNullable(content.getParts()).orElseGet(List::of)))
                .flatMap(Optional::stream)
                .flatMap(List::stream)
                .map(GeminiPartDto::getText)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse("");

        return new UserLlmChatResponseDto(answer);
    }
}
