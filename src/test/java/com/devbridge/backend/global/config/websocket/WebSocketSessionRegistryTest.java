package com.devbridge.backend.global.config.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebSocketSessionRegistryTest {

    private WebSocketSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WebSocketSessionRegistry();
    }

    @Test
    void sendToUser_같은employeeId로두세션등록시_둘다메시지를받는다() throws IOException {
        WebSocketSession session1 = createMockSession("s1", "EMP001", true);
        WebSocketSession session2 = createMockSession("s2", "EMP001", true);

        registry.register("EMP001", session1);
        registry.register("EMP001", session2);

        registry.sendToUser("EMP001", "{\"type\":\"notification\"}");

        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToUser_닫힌세션은제외하고열린세션에만전송한다() throws IOException {
        WebSocketSession openSession = createMockSession("s1", "EMP001", true);
        WebSocketSession closedSession = createMockSession("s2", "EMP001", false);

        registry.register("EMP001", openSession);
        registry.register("EMP001", closedSession);

        registry.sendToUser("EMP001", "{\"type\":\"notification\"}");

        verify(openSession).sendMessage(any(TextMessage.class));
        verify(closedSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToUser_등록되지않은employeeId는아무동작없이종료한다() {
        registry.sendToUser("UNKNOWN", "{\"type\":\"notification\"}");
    }

    @Test
    void unregister_세션해제후해당세션에는전송하지않는다() throws IOException {
        WebSocketSession session = createMockSession("s1", "EMP001", true);

        registry.register("EMP001", session);
        registry.unregister(session);

        registry.sendToUser("EMP001", "{\"type\":\"notification\"}");

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void unregister_같은employeeId의다른세션은유지된다() throws IOException {
        WebSocketSession session1 = createMockSession("s1", "EMP001", true);
        WebSocketSession session2 = createMockSession("s2", "EMP001", true);

        registry.register("EMP001", session1);
        registry.register("EMP001", session2);
        registry.unregister(session1);

        registry.sendToUser("EMP001", "{\"type\":\"notification\"}");

        verify(session1, never()).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

    private WebSocketSession createMockSession(String sessionId, String employeeId, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(open);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("employeeId", employeeId);
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }
}
