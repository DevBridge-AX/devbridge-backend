package com.devbridge.backend.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 채팅 RAG 검색 시 AI 엔진에 전달할 접근 범위.
 * AI 엔진 ChatRequest의 accessible_task_ids / can_view_restricted와 1:1 대응한다.
 */
@Getter
@AllArgsConstructor
public class ChatAccessScope {

    /** null이면 태스크 제한 없음. 목록이면 태스크 미지정 문서 + 목록 내 태스크의 문서만 검색한다. */
    private final List<String> accessibleTaskIds;

    /** false면 restricted 출처를 검색에서 제외한다. */
    private final boolean canViewRestricted;

    /** 제한 없음 + restricted 제외. 접근 제어 도입 이전의 기존 동작과 동일하다. */
    public static ChatAccessScope unrestricted() {
        return new ChatAccessScope(null, false);
    }
}
