package com.devbridge.backend.domain.notification.service;

import com.devbridge.backend.domain.notification.dto.NotificationResponse;
import com.devbridge.backend.domain.notification.entity.Notification;
import com.devbridge.backend.domain.notification.repository.NotificationRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.global.config.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createNotification(User recipient, String type, String referenceId,
                                   String title, String message, String workspaceId) {
        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .referenceId(referenceId)
                .workspaceId(workspaceId)
                .title(title)
                .message(message)
                .build();
        notificationRepository.save(notification);

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "notification");
            payload.put("notification_type", type);
            payload.put("notification_id", notification.getId());
            payload.put("reference_id", referenceId);
            payload.put("title", title);
            payload.put("message", message);
            payload.put("workspace_id", workspaceId);
            payload.put("created_at", notification.getCreatedAt() != null
                    ? notification.getCreatedAt().toString() : null);

            webSocketSessionRegistry.sendToUser(
                    recipient.getEmployeeId(), objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("알림 WebSocket push 실패: recipientEmployeeId={}, type={}, error={}",
                    recipient.getEmployeeId(), type, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(String employeeId, Boolean isRead, Pageable pageable) {
        Page<Notification> page = isRead != null
                ? notificationRepository.findByUser_EmployeeIdAndIsReadAndDeletedAtIsNull(employeeId, isRead, pageable)
                : notificationRepository.findByUser_EmployeeIdAndDeletedAtIsNull(employeeId, pageable);
        return page.map(NotificationResponse::from);
    }

    @Transactional
    public void readNotification(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않습니다."));
        notification.markAsRead();
    }
}
