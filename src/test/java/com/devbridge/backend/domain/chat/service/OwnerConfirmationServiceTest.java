package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.entity.ChatMessage;
import com.devbridge.backend.domain.chat.entity.ChatSession;
import com.devbridge.backend.domain.chat.entity.OwnerConfirmation;
import com.devbridge.backend.domain.chat.repository.ChatMessageRepository;
import com.devbridge.backend.domain.chat.repository.OwnerConfirmationRepository;
import com.devbridge.backend.domain.notification.service.NotificationService;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerConfirmationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private OwnerConfirmationRepository ownerConfirmationRepository;

    @Mock
    private NotificationService notificationService;

    private OwnerConfirmationService ownerConfirmationService;

    @BeforeEach
    void setUp() {
        ownerConfirmationService = new OwnerConfirmationService(
                userRepository, chatMessageRepository, ownerConfirmationRepository, notificationService);
    }

    @Test
    void triggerOwnerConfirmation_OwnerConfirmationRow가저장되고알림이전송된다() {
        User owner = User.builder()
                .id("owner-1").employeeId("EMP010").name("김담당")
                .systemRole("USER").authProvider("LOCAL").build();

        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        ChatSession session = ChatSession.builder()
                .id("session-1").workspace(workspace)
                .user(owner).sessionTitle("테스트 세션").build();
        ChatMessage chatMessage = ChatMessage.builder()
                .id("msg-1").session(session)
                .senderType("USER").content("테스트 질문").build();

        when(userRepository.findById("owner-1")).thenReturn(Optional.of(owner));
        when(chatMessageRepository.findById("msg-1")).thenReturn(Optional.of(chatMessage));
        when(ownerConfirmationRepository.save(any(OwnerConfirmation.class))).thenAnswer(invocation -> {
            OwnerConfirmation oc = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(oc, "id", "oc-1");
            return oc;
        });

        Optional<String> result = ownerConfirmationService.triggerOwnerConfirmation("msg-1", "owner-1");

        assertThat(result).isPresent().contains("김담당");

        ArgumentCaptor<OwnerConfirmation> captor = ArgumentCaptor.forClass(OwnerConfirmation.class);
        verify(ownerConfirmationRepository).save(captor.capture());

        OwnerConfirmation saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getQuestionMessage()).isEqualTo(chatMessage);
        assertThat(saved.getAssignedOwner()).isEqualTo(owner);
        assertThat(saved.getWorkspace()).isEqualTo(workspace);
        assertThat(saved.getRelatedDocument()).isNull();
        assertThat(saved.getQuestionContent()).isNull();
    }

    @Test
    void triggerOwnerConfirmation_notification의referenceId가ownerConfirmationId와일치한다() {
        User owner = User.builder()
                .id("owner-1").employeeId("EMP010").name("김담당")
                .systemRole("USER").authProvider("LOCAL").build();

        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        ChatSession session = ChatSession.builder()
                .id("session-1").workspace(workspace)
                .user(owner).sessionTitle("테스트 세션").build();
        ChatMessage chatMessage = ChatMessage.builder()
                .id("msg-1").session(session)
                .senderType("USER").content("테스트 질문").build();

        when(userRepository.findById("owner-1")).thenReturn(Optional.of(owner));
        when(chatMessageRepository.findById("msg-1")).thenReturn(Optional.of(chatMessage));
        when(ownerConfirmationRepository.save(any(OwnerConfirmation.class))).thenAnswer(invocation -> {
            OwnerConfirmation oc = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(oc, "id", "oc-generated-id");
            return oc;
        });

        ownerConfirmationService.triggerOwnerConfirmation("msg-1", "owner-1");

        verify(notificationService).createNotification(
                eq(owner), eq("OWNER_CONFIRMATION"), eq("oc-generated-id"),
                eq("담당자 확인 요청"),
                eq("문서 근거가 부족한 질문이 배정되었습니다. 확인 후 답변해 주세요."));
    }

    @Test
    void triggerOwnerConfirmation_User가없으면OptionalEmpty반환하고저장도안된다() {
        when(userRepository.findById("unknown-owner")).thenReturn(Optional.empty());

        Optional<String> result = ownerConfirmationService.triggerOwnerConfirmation("msg-1", "unknown-owner");

        assertThat(result).isEmpty();
        verify(ownerConfirmationRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void createOwnerConfirmationFromChat_정상케이스_저장과알림이모두수행된다() {
        User owner = User.builder()
                .id("owner-1").employeeId("EMP010").name("김담당")
                .systemRole("USER").authProvider("LOCAL").build();

        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        ChatSession session = ChatSession.builder()
                .id("session-1").workspace(workspace)
                .user(owner).sessionTitle("테스트 세션").build();
        ChatMessage chatMessage = ChatMessage.builder()
                .id("msg-1").session(session)
                .senderType("USER").content("테스트 질문").build();

        when(ownerConfirmationRepository.existsByQuestionMessage_IdAndStatus("msg-1", "PENDING"))
                .thenReturn(false);
        when(userRepository.findById("owner-1")).thenReturn(Optional.of(owner));
        when(chatMessageRepository.findById("msg-1")).thenReturn(Optional.of(chatMessage));
        when(ownerConfirmationRepository.save(any(OwnerConfirmation.class))).thenAnswer(invocation -> {
            OwnerConfirmation oc = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(oc, "id", "oc-new");
            return oc;
        });

        ownerConfirmationService.createOwnerConfirmationFromChat("msg-1", "owner-1");

        ArgumentCaptor<OwnerConfirmation> captor = ArgumentCaptor.forClass(OwnerConfirmation.class);
        verify(ownerConfirmationRepository).save(captor.capture());

        OwnerConfirmation saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getQuestionMessage()).isEqualTo(chatMessage);
        assertThat(saved.getAssignedOwner()).isEqualTo(owner);
        assertThat(saved.getWorkspace()).isEqualTo(workspace);

        verify(notificationService).createNotification(
                eq(owner), eq("OWNER_CONFIRMATION"), eq("oc-new"),
                eq("담당자 확인 요청"),
                eq("채팅 질문에 대한 확인이 요청되었습니다. 답변해 주세요."));
    }

    @Test
    void createOwnerConfirmationFromChat_중복호출시_IllegalStateException이발생한다() {
        when(ownerConfirmationRepository.existsByQuestionMessage_IdAndStatus("msg-1", "PENDING"))
                .thenReturn(true);

        assertThatThrownBy(() -> ownerConfirmationService.createOwnerConfirmationFromChat("msg-1", "owner-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 담당자가 배정된 질문입니다.");

        verify(ownerConfirmationRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void createOwnerConfirmationFromChat_User가없으면_IllegalArgumentException이발생한다() {
        when(ownerConfirmationRepository.existsByQuestionMessage_IdAndStatus("msg-1", "PENDING"))
                .thenReturn(false);
        when(userRepository.findById("unknown-owner")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownerConfirmationService.createOwnerConfirmationFromChat("msg-1", "unknown-owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 사용자가 존재하지 않습니다: unknown-owner");

        verify(ownerConfirmationRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }
}
