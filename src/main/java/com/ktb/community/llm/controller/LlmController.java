package com.ktb.community.llm.controller;

import com.ktb.community.dto.ApiResponseDto;
import com.ktb.community.llm.dto.UserLlmChatResponseDto;
import com.ktb.community.llm.service.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @PostMapping("/llm")
    public Mono<ApiResponseDto<UserLlmChatResponseDto>> getChat(@RequestParam("roomId") Long roomId,
                                                                @AuthenticationPrincipal(expression = "userId") Long userId) {
        return llmService.getChat(roomId, userId)
                .map(ApiResponseDto::success);
    }
}
