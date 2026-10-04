package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.dto.ChatAccessScope;
import org.springframework.stereotype.Component;

/**
 * 채팅 요청자의 RAG 접근 범위를 산출한다.
 */
@Component
public class ChatAccessScopeResolver {

    /**
     * TODO(K-F6): 태스크 배정 및 워크스페이스 RBAC 기반 산출 로직을 이곳에 채운다.
     * 현재는 제한 없음(unrestricted)을 반환하며, 이는 접근 범위를 전달하지 않던 기존 동작과 동일하다.
     * 핸들러는 이 메서드의 반환값만 사용하므로 산출 로직 교체 시 핸들러 수정은 필요 없다.
     */
    public ChatAccessScope resolve(String workspaceId, String employeeId) {
        return ChatAccessScope.unrestricted();
    }
}
