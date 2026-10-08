package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.dto.ChatAccessScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatAccessScopeResolverTest {

    private final ChatAccessScopeResolver resolver = new ChatAccessScopeResolver();

    @Test
    @DisplayName("resolve_default_returnsUnrestricted")
    void resolve_default_returnsUnrestricted() {
        ChatAccessScope scope = resolver.resolve("WS-001", "EMP001");

        // K-F6 산출 로직 도입 전까지는 기존 동작(제한 없음, restricted 제외)과 같아야 한다.
        assertThat(scope.getAccessibleTaskIds()).isNull();
        assertThat(scope.isCanViewRestricted()).isFalse();
    }
}
