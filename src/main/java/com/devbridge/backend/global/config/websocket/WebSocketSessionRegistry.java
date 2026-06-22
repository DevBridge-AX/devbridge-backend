package com.devbridge.backend.global.config.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class WebSocketSessionRegistry {

    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void register(String employeeId, WebSocketSession session) {
        sessions.computeIfAbsent(employeeId, k -> new CopyOnWriteArrayList<>()).add(session);
        log.info("WebSocket 세션 등록: employeeId={}, sessionId={}", employeeId, session.getId());
    }

    public void unregister(WebSocketSession session) {
        String employeeId = (String) session.getAttributes().get("employeeId");
        if (employeeId != null) {
            CopyOnWriteArrayList<WebSocketSession> list = sessions.get(employeeId);
            if (list != null) {
                list.remove(session);
                if (list.isEmpty()) {
                    sessions.remove(employeeId);
                }
            }
        }
        log.info("WebSocket 세션 해제: employeeId={}, sessionId={}", employeeId, session.getId());
    }

    public void sendToUser(String employeeId, String jsonMessage) {
        CopyOnWriteArrayList<WebSocketSession> list = sessions.get(employeeId);
        if (list == null || list.isEmpty()) {
            return;
        }

        TextMessage textMessage = new TextMessage(jsonMessage);
        list.removeIf(session -> {
            if (!session.isOpen()) {
                log.info("만료된 WebSocket 세션 제거: employeeId={}, sessionId={}", employeeId, session.getId());
                return true;
            }
            try {
                synchronized (session) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                log.error("WebSocket 메시지 전송 실패: employeeId={}, sessionId={}, error={}",
                        employeeId, session.getId(), e.getMessage());
            }
            return false;
        });

        if (list.isEmpty()) {
            sessions.remove(employeeId);
        }
    }
}
