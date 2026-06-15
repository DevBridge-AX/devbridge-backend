package com.devbridge.backend.domain.notification.service;

import com.devbridge.backend.domain.notification.dto.NotificationResponse;
import com.devbridge.backend.domain.notification.entity.Notification;
import com.devbridge.backend.domain.notification.repository.NotificationRepository;
import com.devbridge.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createNotification(User recipient, String type, String referenceId) {
        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .referenceId(referenceId)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public void readNotification(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림이 존재하지 않습니다."));
        notification.markAsRead();
    }
}
