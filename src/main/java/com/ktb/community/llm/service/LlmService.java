package com.ktb.community.llm.service;

import com.ktb.community.chat.entity.ChatMessage;
import com.ktb.community.chat.entity.ChatRoom;
import com.ktb.community.chat.repository.ChatMessageRepository;
import com.ktb.community.chat.repository.ChatRoomRepository;
import com.ktb.community.exception.BusinessException;
import com.ktb.community.llm.dto.GeminiContentDto;
import com.ktb.community.llm.dto.GeminiGenerateContentRequestDto;
import com.ktb.community.llm.dto.GeminiGenerateContentResponseDto;
import com.ktb.community.llm.dto.GeminiPartDto;
import com.ktb.community.llm.dto.UserLlmChatResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.ktb.community.exception.ErrorCode.ROOM_NOT_FOUND;
import static reactor.core.scheduler.Schedulers.boundedElastic;

@Service
@RequiredArgsConstructor
public class LlmService {

    private static final String GEMINI_GENERATE_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s";

    private final WebClient webClient;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${llm.gemini.key}")
    private String geminiApiKey;

    public Mono<UserLlmChatResponseDto> getChat(Long roomId) {
        return Mono.fromCallable(() -> fetchRecentMessages(roomId))
                .subscribeOn(boundedElastic())
                .map(this::buildGeminiRequest)
                .flatMap(this::callGemini)
                .map(this::toUserResponse);
    }

    private List<ChatMessage> fetchRecentMessages(Long roomId) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new BusinessException(ROOM_NOT_FOUND));
            List<ChatMessage> recentMessages = chatMessageRepository.findTop10ByChatRoomOrderByCreatedAtDesc(chatRoom);
            Collections.reverse(recentMessages); // 가장 오래된 것부터 정렬
            return recentMessages;
        }));
    }

    private GeminiGenerateContentRequestDto buildGeminiRequest(List<ChatMessage> messages) {
        List<GeminiContentDto> contents = new ArrayList<>();
        contents.add(GeminiContentDto.user("너는 두 명의 사용자가 대화 중인 채팅방의 문맥을 이해하고, 상대방의 마지막 대화메시지에 맥락에 맞는 응답을 하는 한국어 어시스턴트야. 메시지는 [닉네임]: 내용 형식으로 제공돼."));

        for (ChatMessage message : messages) {
            contents.add(GeminiContentDto.user(formatMessageWithSender(message)));
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
        String uri = GEMINI_GENERATE_ENDPOINT.formatted(geminiApiKey);
        return webClient.post()
                .uri(uri)
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
