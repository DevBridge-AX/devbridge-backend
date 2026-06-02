package com.devbridge.backend.api.notification;

import com.devbridge.backend.domain.notification.dto.NotificationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class NotificationController implements NotificationAPI {

    @Override
    public ResponseEntity<List<NotificationResponse>> getNotifications(String userId) {
        List<NotificationResponse> list = List.of(
                NotificationResponse.builder()
                        .id("notif-1")
                        .userId(userId)
                        .type("TASK_ASSIGNED")
                        .referenceId("dummy-task-id")
                        .isRead(false)
                        .build()
        );
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Void> readNotification(String id) {
        return ResponseEntity.ok().build();
    }
}
