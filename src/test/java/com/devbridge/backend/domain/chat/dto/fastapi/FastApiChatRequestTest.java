package com.devbridge.backend.domain.chat.dto.fastapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FastApiChatRequest 직렬화 키는 AI 엔진 ChatRequest와의 계약이다.
 * 앱의 ObjectMapper 커스텀 설정이 없어 기본 ObjectMapper로 검증한다.
 */
class FastApiChatRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("serialize_withAccessScope_usesSnakeCaseKeys")
    void serialize_withAccessScope_usesSnakeCaseKeys() throws Exception {
        FastApiChatRequest request = FastApiChatRequest.builder()
                .sessionId("S-1")
                .accessibleTaskIds(List.of("TASK-1"))
                .canViewRestricted(true)
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(json.path("accessible_task_ids").get(0).asText()).isEqualTo("TASK-1");
        assertThat(json.path("can_view_restricted").asBoolean()).isTrue();
        assertThat(json.has("accessibleTaskIds")).isFalse();
        assertThat(json.has("canViewRestricted")).isFalse();
    }

    @Test
    @DisplayName("serialize_withUnrestrictedScope_serializesNullTaskIdsAndFalse")
    void serialize_withUnrestrictedScope_serializesNullTaskIdsAndFalse() throws Exception {
        FastApiChatRequest request = FastApiChatRequest.builder().sessionId("S-1").build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(request));

        // null이면 FastAPI에서 "태스크 제한 없음"으로 해석된다.
        assertThat(json.path("accessible_task_ids").isNull()).isTrue();
        assertThat(json.path("can_view_restricted").asBoolean(true)).isFalse();
    }
}
