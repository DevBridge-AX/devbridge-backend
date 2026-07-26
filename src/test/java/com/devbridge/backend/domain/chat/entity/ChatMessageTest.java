package com.devbridge.backend.domain.chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ChatMessage}의 식별자·Persistable 로직 특성 테스트(characterization test).
 *
 * <p><b>이 로직은 과거 두 번 깨졌다.</b>
 * <ul>
 *   <li>2026-06-21 `ChatMessage Persistable 적용 및 ID생성 수정`</li>
 *   <li>2026-06-22 `스트리밍 messageId DB저장 ID 일치 수정` —
 *       스트리밍 중 프론트에 보낸 messageId와 DB에 저장된 ID가 달라지던 버그</li>
 * </ul>
 *
 * <p>핵심 계약: <b>ID를 직접 지정하면 절대 덮어쓰지 않는다.</b>
 * `ChatWebSocketHandler`가 스트리밍 시작 시 만든 UUID를 `token` 이벤트로 먼저 내보내고,
 * 나중에 같은 값으로 저장해야 프론트가 말풍선을 매칭할 수 있다.
 * `@GeneratedValue`를 되살리거나 `assignIdIfAbsent`의 null 검사를 없애면 이 계약이 깨진다.
 *
 * <p>JPA 콜백이 private 메서드라 리플렉션으로 호출한다. 실제 영속화 동작까지 확인하려면
 * `@DataJpaTest` 기반 테스트가 추가로 필요하다(현재 레포에는 없음).
 */
class ChatMessageTest {

    private void invokeCallback(ChatMessage message, String methodName) throws Exception {
        Method method = ChatMessage.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(message);
    }

    @Test
    @DisplayName("assignIdIfAbsent_withPresetId_keepsGivenId")
    void assignIdIfAbsent_withPresetId_keepsGivenId() throws Exception {
        String streamingId = "11111111-2222-3333-4444-555555555555";
        ChatMessage message = ChatMessage.builder().id(streamingId).senderType("AI").content("본문").build();

        invokeCallback(message, "assignIdIfAbsent");

        // 2026-06-22 버그의 재발 방지선: 지정된 ID를 절대 새로 생성하지 않는다.
        assertThat(message.getId()).isEqualTo(streamingId);
    }

    @Test
    @DisplayName("assignIdIfAbsent_withoutId_generatesUuid")
    void assignIdIfAbsent_withoutId_generatesUuid() throws Exception {
        ChatMessage message = ChatMessage.builder().senderType("USER").content("질문").build();
        assertThat(message.getId()).isNull();

        invokeCallback(message, "assignIdIfAbsent");

        // saveUserMessage처럼 ID를 지정하지 않는 경로에서만 UUID가 생성된다.
        assertThat(message.getId())
                .isNotNull()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("isNew_beforePersist_returnsTrue")
    void isNew_beforePersist_returnsTrue() {
        ChatMessage message = ChatMessage.builder().senderType("USER").content("질문").build();

        // isNew=true여야 Spring Data JPA가 merge가 아닌 persist(INSERT)를 선택한다.
        // false가 되면 직접 지정한 ID에 대해 "Detached entity" 오류나 SELECT 선행이 발생한다.
        assertThat(message.isNew()).isTrue();
    }

    @Test
    @DisplayName("markNotNew_afterPersist_returnsFalse")
    void markNotNew_afterPersist_returnsFalse() throws Exception {
        ChatMessage message = ChatMessage.builder().senderType("USER").content("질문").build();

        invokeCallback(message, "markNotNew");

        // @PostPersist / @PostLoad 이후에는 새 엔티티가 아니다.
        assertThat(message.isNew()).isFalse();
    }

    @Test
    @DisplayName("build_withoutTokenCounts_defaultsToZero")
    void build_withoutTokenCounts_defaultsToZero() {
        ChatMessage message = ChatMessage.builder().senderType("USER").content("질문").build();

        assertThat(message.getPromptTokens()).isZero();
        assertThat(message.getCompletionTokens()).isZero();
        assertThat(message.getContextTruncated()).isFalse();
        assertThat(message.getFeedback()).isEqualTo(FeedbackType.NONE);
    }
}
