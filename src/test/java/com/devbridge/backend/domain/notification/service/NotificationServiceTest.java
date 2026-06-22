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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
                "회의 일정이 변경되었습니다", "참여 중인 회의의 상세 정보가 변경되었습니다.",
                "workspace-1");

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
                "회의 일정이 변경되었습니다", "참여 중인 회의의 상세 정보가 변경되었습니다.",
                "workspace-1");

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
                "회의 일정이 변경되었습니다", "상세 정보가 변경되었습니다.",
                "workspace-1");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createNotification_WebSocketPayload에workspaceId가포함된다() throws Exception {
        User recipient = createTestUser();

        notificationService.createNotification(
                recipient, "MEETING_INVITED", "meeting-1",
                "회의에 초대되었습니다", "새 회의에 참석자로 초대되었습니다.",
                "workspace-123");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSocketSessionRegistry).sendToUser(eq("EMP002"), payloadCaptor.capture());

        Map<String, Object> payload = objectMapper.readValue(payloadCaptor.getValue(), Map.class);
        assertThat(payload).containsEntry("workspace_id", "workspace-123");
        assertThat(payload).containsEntry("type", "notification");
        assertThat(payload).containsEntry("notification_type", "MEETING_INVITED");
    }

    @Test
    void getNotifications_isReadFalse시_미읽은알림만반환된다() {
        User recipient = createTestUser();
        Notification unread = Notification.builder()
                .id("n-1").user(recipient).type("MEETING_INVITED")
                .referenceId("m-1").title("회의 초대").message("초대됨").build();

        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(notificationRepository.findByUser_EmployeeIdAndIsReadAndDeletedAtIsNull("EMP002", false, pageable))
                .thenReturn(new PageImpl<>(List.of(unread), pageable, 1));

        Page<NotificationResponse> result = notificationService.getNotifications("EMP002", false, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsRead()).isFalse();
        verify(notificationRepository).findByUser_EmployeeIdAndIsReadAndDeletedAtIsNull("EMP002", false, pageable);
    }

    @Test
    void getNotifications_isReadNull시_전체알림이반환된다() {
        User recipient = createTestUser();
        Notification n1 = Notification.builder()
                .id("n-1").user(recipient).type("MEETING_INVITED")
                .referenceId("m-1").title("초대").message("초대됨").build();
        Notification n2 = Notification.builder()
                .id("n-2").user(recipient).type("MEETING_UPDATED")
                .referenceId("m-2").title("변경").message("변경됨").build();

        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(notificationRepository.findByUser_EmployeeIdAndDeletedAtIsNull("EMP002", pageable))
                .thenReturn(new PageImpl<>(List.of(n1, n2), pageable, 2));

        Page<NotificationResponse> result = notificationService.getNotifications("EMP002", null, pageable);

        assertThat(result.getContent()).hasSize(2);
        verify(notificationRepository).findByUser_EmployeeIdAndDeletedAtIsNull("EMP002", pageable);
        verify(notificationRepository, never()).findByUser_EmployeeIdAndIsReadAndDeletedAtIsNull(any(), any(), any());
    }

    @Test
    void getNotifications_페이징이적용된다() {
        User recipient = createTestUser();
        Notification n1 = Notification.builder()
                .id("n-1").user(recipient).type("MEETING_INVITED")
                .referenceId("m-1").title("초대").message("초대됨").build();

        PageRequest pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(notificationRepository.findByUser_EmployeeIdAndDeletedAtIsNull("EMP002", pageable))
                .thenReturn(new PageImpl<>(List.of(n1), pageable, 6));

        Page<NotificationResponse> result = notificationService.getNotifications("EMP002", null, pageable);

        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(6);
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
