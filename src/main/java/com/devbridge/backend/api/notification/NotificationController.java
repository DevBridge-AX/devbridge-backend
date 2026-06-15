package com.devbridge.backend.api.notification;

import com.devbridge.backend.domain.notification.dto.NotificationResponse;
import com.devbridge.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationAPI {

    private final NotificationService notificationService;

    @Override
    public ResponseEntity<List<NotificationResponse>> getNotifications(String userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @Override
    public ResponseEntity<Void> readNotification(String id) {
        notificationService.readNotification(id);
        return ResponseEntity.ok().build();
    }
}
