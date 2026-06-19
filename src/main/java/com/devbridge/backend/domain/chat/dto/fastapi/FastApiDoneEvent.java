package com.devbridge.backend.domain.chat.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FastApiDoneEvent {

    private List<CitationData> citations;

    @JsonProperty("is_groundable")
    private Boolean isGroundable;

    private Double confidence;

    @JsonProperty("suggested_owner_id")
    private String suggestedOwnerId;

    @JsonProperty("prompt_version")
    private String promptVersion;

    @JsonProperty("token_usage")
    private TokenUsage tokenUsage;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitationData {
        @JsonProperty("source_type")
        private String sourceType;

        @JsonProperty("source_id")
        private Integer sourceId;

        @JsonProperty("chunk_id")
        private Integer chunkId;

        private String title;

        @JsonProperty("similarity_score")
        private Double similarityScore;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        private TokenUsageDetail main;
        private TokenUsageDetail rewrite;

        @JsonProperty("context_truncated")
        private Boolean contextTruncated;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsageDetail {
        private String model;

        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;
    }
}
