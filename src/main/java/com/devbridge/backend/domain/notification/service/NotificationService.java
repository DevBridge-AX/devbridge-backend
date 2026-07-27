package com.devbridge.backend.domain.notification.service;

import com.devbridge.backend.domain.notification.dto.NotificationResponse;
import com.devbridge.backend.domain.notification.entity.Notification;
import com.devbridge.backend.domain.notification.repository.NotificationRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.global.common.exception.ForbiddenException;
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
    public Page<NotificationResponse> getNotifications(String employeeId, String requesterEmployeeId,
                                                       Boolean isRead, Pageable pageable) {
        // employeeId는 경로 변수로 전달되는 클라이언트 입력이므로 본인 알림인지 확인한다.
        if (!employeeId.equals(requesterEmployeeId)) {
            throw new ForbiddenException("본인의 알림만 조회할 수 있습니다.");
        }

        Page<Notification> page = isRead != null
                ? notificationRepository.findByUser_EmployeeIdAndIsReadAndDeletedAtIsNull(employeeId, isRead, pageable)
                : notificationRepository.findByUser_EmployeeIdAndDeletedAtIsNull(employeeId, pageable);
        return page.map(NotificationResponse::from);
    }

    @Transactional
    public void readNotification(String id, String employeeId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않습니다."));

        // 알림 ID는 경로 변수로 전달되는 클라이언트 입력이므로 수신자 본인인지 확인한다.
        if (!notification.getUser().getEmployeeId().equals(employeeId)) {
            throw new ForbiddenException("해당 알림에 대한 접근 권한이 없습니다.");
        }

        notification.markAsRead();
    }
}
