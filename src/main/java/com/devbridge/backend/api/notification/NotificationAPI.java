package com.devbridge.backend.api.notification;

import com.devbridge.backend.domain.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Notification", description = "시스템 알림 API 명세")
@RequestMapping("/api/notifications")
public interface NotificationAPI {

    @Operation(summary = "알림 목록 조회", description = "특정 사용자의 전체 알림 수신 내역을 조회합니다.")
    @GetMapping("/users/{userId}")
    ResponseEntity<List<NotificationResponse>> getNotifications(@PathVariable("userId") String userId);

    @Operation(summary = "알림 읽음 처리", description = "알림을 확인한 경우 읽음 상태로 처리합니다.")
    @PutMapping("/{id}/read")
    ResponseEntity<Void> readNotification(@PathVariable("id") String id);
}
