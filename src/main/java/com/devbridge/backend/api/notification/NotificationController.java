package com.devbridge.backend.api.notification;

import com.devbridge.backend.domain.notification.dto.NotificationResponse;
import com.devbridge.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationAPI {

    private final NotificationService notificationService;

    @Override
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            String employeeId, String requesterEmployeeId, Boolean isRead, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(
                notificationService.getNotifications(employeeId, requesterEmployeeId, isRead, pageable));
    }

    @Override
    public ResponseEntity<Void> readNotification(String id, String employeeId) {
        notificationService.readNotification(id, employeeId);
        return ResponseEntity.ok().build();
    }
}
