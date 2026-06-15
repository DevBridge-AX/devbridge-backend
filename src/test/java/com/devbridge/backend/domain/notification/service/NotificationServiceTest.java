package com.devbridge.backend.domain.notification.service;

import com.devbridge.backend.domain.notification.dto.NotificationResponse;
import com.devbridge.backend.domain.notification.entity.Notification;
import com.devbridge.backend.domain.notification.repository.NotificationRepository;
import com.devbridge.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void createNotification_수신자와타입_참조ID로알림을저장한다() {
        User recipient = User.builder().id("user-1").employeeId("EMP002").name("홍길동").systemRole("USER").authProvider("LOCAL").build();

        notificationService.createNotification(recipient, "MEETING_UPDATED", "meeting-1");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(recipient);
        assertThat(saved.getType()).isEqualTo("MEETING_UPDATED");
        assertThat(saved.getReferenceId()).isEqualTo("meeting-1");
    }

    @Test
    void getNotifications_사용자의알림목록을최신순으로반환한다() {
        User recipient = User.builder().id("user-1").employeeId("EMP002").name("홍길동").systemRole("USER").authProvider("LOCAL").build();
        Notification notification = Notification.builder()
                .id("notification-1")
                .user(recipient)
                .type("MEETING_UPDATED")
                .referenceId("meeting-1")
                .build();

        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getNotifications("user-1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo("notification-1");
        assertThat(responses.get(0).getUserId()).isEqualTo("user-1");
        assertThat(responses.get(0).getType()).isEqualTo("MEETING_UPDATED");
        assertThat(responses.get(0).getReferenceId()).isEqualTo("meeting-1");
        assertThat(responses.get(0).getIsRead()).isFalse();
    }

    @Test
    void readNotification_존재하면읽음처리한다() {
        User recipient = User.builder().id("user-1").employeeId("EMP002").name("홍길동").systemRole("USER").authProvider("LOCAL").build();
        Notification notification = Notification.builder()
                .id("notification-1")
                .user(recipient)
                .type("MEETING_UPDATED")
                .referenceId("meeting-1")
                .build();

        when(notificationRepository.findById("notification-1")).thenReturn(Optional.of(notification));

        notificationService.readNotification("notification-1");

        assertThat(notification.getIsRead()).isTrue();
    }

    @Test
    void readNotification_존재하지않으면예외가발생한다() {
        when(notificationRepository.findById("notification-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.readNotification("notification-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 알림이 존재하지 않습니다.");
    }
}
