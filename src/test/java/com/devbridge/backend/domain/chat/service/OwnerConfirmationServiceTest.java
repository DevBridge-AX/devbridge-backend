package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.entity.ChatMessage;
import com.devbridge.backend.domain.chat.entity.ChatSession;
import com.devbridge.backend.domain.chat.entity.OwnerConfirmation;
import com.devbridge.backend.domain.chat.repository.ChatMessageRepository;
import com.devbridge.backend.domain.chat.repository.OwnerConfirmationRepository;
import com.devbridge.backend.domain.datasource.entity.DataSource;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
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
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private NotificationService notificationService;

    private OwnerConfirmationService ownerConfirmationService;

    @BeforeEach
    void setUp() {
        ownerConfirmationService = new OwnerConfirmationService(
                userRepository, chatMessageRepository, ownerConfirmationRepository,
                knowledgeDocumentRepository, notificationService);
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
    void createOwnerConfirmationFromDocument_정상케이스_저장과알림이수행되고relatedDocument가연결된다() {
        User uploader = User.builder()
                .id("uploader-1").employeeId("EMP020").name("박등록")
                .systemRole("USER").authProvider("LOCAL").build();
        User requester = User.builder()
                .id("requester-1").employeeId("EMP030").name("이요청")
                .systemRole("USER").authProvider("LOCAL").build();

        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        DataSource dataSource = DataSource.builder()
                .id("ds-1").workspace(workspace).sourceType("DOC").sourceName("테스트 소스").build();
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id("doc-1").dataSource(dataSource).uploadedBy(uploader).title("설계 문서").build();

        when(knowledgeDocumentRepository.findById("doc-1")).thenReturn(Optional.of(document));
        when(userRepository.findByEmployeeId("EMP030")).thenReturn(Optional.of(requester));
        when(ownerConfirmationRepository.save(any(OwnerConfirmation.class))).thenAnswer(invocation -> {
            OwnerConfirmation oc = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(oc, "id", "oc-doc-1");
            return oc;
        });

        ownerConfirmationService.createOwnerConfirmationFromDocument("doc-1", "이 문서에 대해 질문이 있습니다", "EMP030");

        ArgumentCaptor<OwnerConfirmation> captor = ArgumentCaptor.forClass(OwnerConfirmation.class);
        verify(ownerConfirmationRepository).save(captor.capture());

        OwnerConfirmation saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getRelatedDocument()).isEqualTo(document);
        assertThat(saved.getQuestionContent()).isEqualTo("이 문서에 대해 질문이 있습니다");
        assertThat(saved.getAssignedOwner()).isEqualTo(uploader);
        assertThat(saved.getWorkspace()).isEqualTo(workspace);
        assertThat(saved.getQuestionMessage()).isNull();

        verify(notificationService).createNotification(
                eq(uploader), eq("OWNER_CONFIRMATION"), eq("oc-doc-1"),
                eq("문서 관련 질문이 도착했습니다"),
                eq("이요청님이 [설계 문서] 문서에 대해 질문을 남겼습니다."));
    }

    @Test
    void createOwnerConfirmationFromDocument_uploadedBy가null이면_IllegalArgumentException이발생한다() {
        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        DataSource dataSource = DataSource.builder()
                .id("ds-1").workspace(workspace).sourceType("DOC").sourceName("테스트 소스").build();
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id("doc-1").dataSource(dataSource).uploadedBy(null).title("설계 문서").build();

        when(knowledgeDocumentRepository.findById("doc-1")).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> ownerConfirmationService.createOwnerConfirmationFromDocument(
                "doc-1", "질문입니다", "EMP030"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("등록자 정보가 없는 문서입니다. 담당자를 지정할 수 없습니다.");

        verify(ownerConfirmationRepository, never()).save(any());
    }

    @Test
    void createOwnerConfirmationFromDocument_본인문서요청시_IllegalArgumentException이발생한다() {
        User uploader = User.builder()
                .id("same-user").employeeId("EMP020").name("박등록")
                .systemRole("USER").authProvider("LOCAL").build();

        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        DataSource dataSource = DataSource.builder()
                .id("ds-1").workspace(workspace).sourceType("DOC").sourceName("테스트 소스").build();
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id("doc-1").dataSource(dataSource).uploadedBy(uploader).title("설계 문서").build();

        when(knowledgeDocumentRepository.findById("doc-1")).thenReturn(Optional.of(document));
        when(userRepository.findByEmployeeId("EMP020")).thenReturn(Optional.of(uploader));

        assertThatThrownBy(() -> ownerConfirmationService.createOwnerConfirmationFromDocument(
                "doc-1", "질문입니다", "EMP020"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인이 등록한 문서입니다.");

        verify(ownerConfirmationRepository, never()).save(any());
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
