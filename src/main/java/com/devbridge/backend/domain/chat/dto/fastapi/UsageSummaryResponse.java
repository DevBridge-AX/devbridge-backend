package com.devbridge.backend.domain.chat.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UsageSummaryResponse {

    @JsonProperty("workspace_id")
    private String workspaceId;

    private List<UsageRecord> records;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageRecord {
        private String date;

        @JsonProperty("embedding_model")
        private String embeddingModel;

        @JsonProperty("embedding_tokens")
        private Integer embeddingTokens;
    }
}
