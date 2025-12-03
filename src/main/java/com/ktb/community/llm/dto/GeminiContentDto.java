package com.ktb.community.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiContentDto {
    private String role;
    private List<GeminiPartDto> parts;

    public static GeminiContentDto user(String text) {
        return GeminiContentDto.builder()
                .role("user")
                .parts(List.of(GeminiPartDto.builder().text(text).build()))
                .build();
    }
}
