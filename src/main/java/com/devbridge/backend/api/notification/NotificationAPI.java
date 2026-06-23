package com.devbridge.backend.api.notification;

import com.devbridge.backend.domain.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "시스템 알림 API 명세")
@RequestMapping("/api/notifications")
public interface NotificationAPI {

    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 수신 내역을 조회합니다. isRead 파라미터로 읽음 여부 필터링, page/size로 페이징을 지원합니다.")
    @GetMapping("/users/{employeeId}")
    ResponseEntity<Page<NotificationResponse>> getNotifications(
            @PathVariable("employeeId") String employeeId,
            @RequestParam(value = "isRead", required = false) Boolean isRead,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size);

    @Operation(summary = "알림 읽음 처리", description = "알림을 확인한 경우 읽음 상태로 처리합니다.")
    @PutMapping("/{id}/read")
    ResponseEntity<Void> readNotification(@PathVariable("id") String id);
}
