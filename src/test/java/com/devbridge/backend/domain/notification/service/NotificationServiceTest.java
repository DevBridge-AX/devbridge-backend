package com.devbridge.backend.domain.notification.service;

import com.devbridge.backend.domain.notification.dto.NotificationResponse;
import com.devbridge.backend.domain.notification.entity.Notification;
import com.devbridge.backend.domain.notification.repository.NotificationRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.global.config.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WebSocketSessionRegistry webSocketSessionRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository, webSocketSessionRegistry, objectMapper);
    }

    @Test
    void createNotification_DB저장과WebSocketPush가모두호출된다() {
        User recipient = createTestUser();

        notificationService.createNotification(
                recipient, "MEETING_UPDATED", "meeting-1",
                "회의 일정이 변경되었습니다", "참여 중인 회의의 상세 정보가 변경되었습니다.");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        verify(webSocketSessionRegistry).sendToUser(eq("EMP002"), anyString());

        Notification saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(recipient);
        assertThat(saved.getType()).isEqualTo("MEETING_UPDATED");
        assertThat(saved.getReferenceId()).isEqualTo("meeting-1");
    }

    @Test
    void createNotification_엔티티에title과message가정확히저장된다() {
        User recipient = createTestUser();

        notificationService.createNotification(
                recipient, "MEETING_UPDATED", "meeting-1",
                "회의 일정이 변경되었습니다", "참여 중인 회의의 상세 정보가 변경되었습니다.");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("회의 일정이 변경되었습니다");
        assertThat(saved.getMessage()).isEqualTo("참여 중인 회의의 상세 정보가 변경되었습니다.");
    }

    @Test
    void createNotification_pushExceptionThrown_DB저장은성공한다() {
        User recipient = createTestUser();
        doThrow(new RuntimeException("WebSocket 전송 실패"))
                .when(webSocketSessionRegistry).sendToUser(anyString(), anyString());

        notificationService.createNotification(
                recipient, "MEETING_UPDATED", "meeting-1",
                "회의 일정이 변경되었습니다", "상세 정보가 변경되었습니다.");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void getNotifications_사용자의알림목록을최신순으로반환한다() {
        User recipient = createTestUser();
        Notification notification = Notification.builder()
                .id("notification-1")
                .user(recipient)
                .type("MEETING_UPDATED")
                .referenceId("meeting-1")
                .title("회의 일정이 변경되었습니다")
                .message("상세 정보가 변경되었습니다.")
                .build();

        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getNotifications("user-1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo("notification-1");
        assertThat(responses.get(0).getUserId()).isEqualTo("user-1");
        assertThat(responses.get(0).getType()).isEqualTo("MEETING_UPDATED");
        assertThat(responses.get(0).getReferenceId()).isEqualTo("meeting-1");
        assertThat(responses.get(0).getTitle()).isEqualTo("회의 일정이 변경되었습니다");
        assertThat(responses.get(0).getMessage()).isEqualTo("상세 정보가 변경되었습니다.");
        assertThat(responses.get(0).getIsRead()).isFalse();
    }

    @Test
    void readNotification_존재하면읽음처리한다() {
        User recipient = createTestUser();
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

    private User createTestUser() {
        return User.builder()
                .id("user-1")
                .employeeId("EMP002")
                .name("홍길동")
                .systemRole("USER")
                .authProvider("LOCAL")
                .build();
    }
}
