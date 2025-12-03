package com.ktb.community.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiGenerateContentResponseDto {
    private List<GeminiCandidateDto> candidates;
}
