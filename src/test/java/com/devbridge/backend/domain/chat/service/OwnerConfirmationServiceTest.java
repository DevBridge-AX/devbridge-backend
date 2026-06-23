package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.dto.OwnerConfirmationResponse;
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
import com.devbridge.backend.global.config.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private WebSocketSessionRegistry webSocketSessionRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OwnerConfirmationService ownerConfirmationService;

    @BeforeEach
    void setUp() {
        ownerConfirmationService = new OwnerConfirmationService(
                userRepository, chatMessageRepository, ownerConfirmationRepository,
                knowledgeDocumentRepository, notificationService,
                webSocketSessionRegistry, objectMapper);
    }

    // --- triggerOwnerConfirmation ---

    @Test
    void triggerOwnerConfirmation_OwnerConfirmationRow가저장되고requester가세팅된다() {
        User owner = createUser("owner-1", "EMP010", "김담당");
        User sessionUser = createUser("user-1", "EMP001", "홍질문");

        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        ChatSession session = ChatSession.builder()
                .id("session-1").workspace(workspace)
                .user(sessionUser).sessionTitle("테스트 세션").build();
        ChatMessage chatMessage = ChatMessage.builder()
                .id("msg-1").session(session)
                .senderType("USER").content("테스트 질문").build();

        when(userRepository.findById("owner-1")).thenReturn(Optional.of(owner));
        when(chatMessageRepository.findById("msg-1")).thenReturn(Optional.of(chatMessage));
        when(ownerConfirmationRepository.save(any(OwnerConfirmation.class))).thenAnswer(invocation -> {
            OwnerConfirmation oc = invocation.getArgument(0);
            ReflectionTestUtils.setField(oc, "id", "oc-1");
            return oc;
        });

        Optional<String> result = ownerConfirmationService.triggerOwnerConfirmation("msg-1", "owner-1");

        assertThat(result).isPresent().contains("김담당");

        ArgumentCaptor<OwnerConfirmation> captor = ArgumentCaptor.forClass(OwnerConfirmation.class);
        verify(ownerConfirmationRepository).save(captor.capture());

        OwnerConfirmation saved = captor.getValue();
        assertThat(saved.getAssignedOwner()).isEqualTo(owner);
        assertThat(saved.getRequester()).isEqualTo(sessionUser);
        assertThat(saved.getWorkspace()).isEqualTo(workspace);
    }

    @Test
    void triggerOwnerConfirmation_User가없으면OptionalEmpty반환하고저장도안된다() {
        when(userRepository.findById("unknown-owner")).thenReturn(Optional.empty());

        Optional<String> result = ownerConfirmationService.triggerOwnerConfirmation("msg-1", "unknown-owner");

        assertThat(result).isEmpty();
        verify(ownerConfirmationRepository, never()).save(any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    // --- createOwnerConfirmationFromChat ---

    @Test
    void createOwnerConfirmationFromChat_정상케이스_requester가저장된다() {
        User owner = createUser("owner-1", "EMP010", "김담당");
        User requester = createUser("requester-1", "EMP001", "홍질문");

        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        ChatSession session = ChatSession.builder()
                .id("session-1").workspace(workspace)
                .user(requester).sessionTitle("테스트 세션").build();
        ChatMessage chatMessage = ChatMessage.builder()
                .id("msg-1").session(session)
                .senderType("USER").content("테스트 질문").build();

        when(ownerConfirmationRepository.existsByQuestionMessage_IdAndStatus("msg-1", "PENDING"))
                .thenReturn(false);
        when(userRepository.findById("owner-1")).thenReturn(Optional.of(owner));
        when(chatMessageRepository.findById("msg-1")).thenReturn(Optional.of(chatMessage));
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(requester));
        when(ownerConfirmationRepository.save(any(OwnerConfirmation.class))).thenAnswer(invocation -> {
            OwnerConfirmation oc = invocation.getArgument(0);
            ReflectionTestUtils.setField(oc, "id", "oc-new");
            return oc;
        });

        ownerConfirmationService.createOwnerConfirmationFromChat("msg-1", "owner-1", "EMP001");

        ArgumentCaptor<OwnerConfirmation> captor = ArgumentCaptor.forClass(OwnerConfirmation.class);
        verify(ownerConfirmationRepository).save(captor.capture());

        OwnerConfirmation saved = captor.getValue();
        assertThat(saved.getRequester()).isEqualTo(requester);
        assertThat(saved.getAssignedOwner()).isEqualTo(owner);

        verify(notificationService).createNotification(
                eq(owner), eq("OWNER_CONFIRMATION"), eq("oc-new"),
                eq("담당자 확인 요청"),
                eq("채팅 질문에 대한 확인이 요청되었습니다. 답변해 주세요."),
                eq("ws-1"));
    }

    @Test
    void createOwnerConfirmationFromChat_중복호출시_IllegalStateException이발생한다() {
        when(ownerConfirmationRepository.existsByQuestionMessage_IdAndStatus("msg-1", "PENDING"))
                .thenReturn(true);

        assertThatThrownBy(() -> ownerConfirmationService.createOwnerConfirmationFromChat("msg-1", "owner-1", "EMP001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 담당자가 배정된 질문입니다.");

        verify(ownerConfirmationRepository, never()).save(any());
    }

    // --- createOwnerConfirmationFromDocument ---

    @Test
    void createOwnerConfirmationFromDocument_정상케이스_requester가저장된다() {
        User uploader = createUser("uploader-1", "EMP020", "박등록");
        User requester = createUser("requester-1", "EMP030", "이요청");

        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        DataSource dataSource = DataSource.builder()
                .id("ds-1").workspace(workspace).sourceType("DOC").sourceName("테스트 소스").build();
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id("doc-1").dataSource(dataSource).uploadedBy(uploader).title("설계 문서").build();

        when(knowledgeDocumentRepository.findById("doc-1")).thenReturn(Optional.of(document));
        when(userRepository.findByEmployeeId("EMP030")).thenReturn(Optional.of(requester));
        when(ownerConfirmationRepository.save(any(OwnerConfirmation.class))).thenAnswer(invocation -> {
            OwnerConfirmation oc = invocation.getArgument(0);
            ReflectionTestUtils.setField(oc, "id", "oc-doc-1");
            return oc;
        });

        ownerConfirmationService.createOwnerConfirmationFromDocument("doc-1", "질문입니다", "EMP030");

        ArgumentCaptor<OwnerConfirmation> captor = ArgumentCaptor.forClass(OwnerConfirmation.class);
        verify(ownerConfirmationRepository).save(captor.capture());

        OwnerConfirmation saved = captor.getValue();
        assertThat(saved.getRequester()).isEqualTo(requester);
        assertThat(saved.getAssignedOwner()).isEqualTo(uploader);
        assertThat(saved.getRelatedDocument()).isEqualTo(document);

        verify(notificationService).createNotification(
                eq(uploader), eq("OWNER_CONFIRMATION"), eq("oc-doc-1"),
                eq("문서 관련 질문이 도착했습니다"),
                eq("이요청님이 [설계 문서] 문서에 대해 질문을 남겼습니다."),
                eq("ws-1"));
    }

    @Test
    void createOwnerConfirmationFromDocument_본인문서요청시_예외가발생한다() {
        User uploader = createUser("same-user", "EMP020", "박등록");

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
    }

    // --- submitAnswer ---

    @Test
    @SuppressWarnings("unchecked")
    void submitAnswer_정상케이스_상태변경과알림및WebSocket이전송된다() throws Exception {
        User owner = createUser("owner-1", "EMP010", "김담당");
        User requester = createUser("requester-1", "EMP001", "홍질문");
        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();

        ChatMessage questionMsg = ChatMessage.builder()
                .id("msg-1").senderType("USER").content("테스트 질문").build();

        OwnerConfirmation confirmation = OwnerConfirmation.builder()
                .workspace(workspace)
                .questionMessage(questionMsg)
                .assignedOwner(owner)
                .requester(requester)
                .build();
        ReflectionTestUtils.setField(confirmation, "id", "oc-1");

        when(ownerConfirmationRepository.findByIdAndDeletedAtIsNull("oc-1"))
                .thenReturn(Optional.of(confirmation));

        OwnerConfirmationResponse response = ownerConfirmationService.submitAnswer("oc-1", "EMP010", "답변 내용입니다.");

        assertThat(confirmation.getStatus()).isEqualTo("ANSWERED");
        assertThat(confirmation.getAnswerContent()).isEqualTo("답변 내용입니다.");
        assertThat(confirmation.getAnsweredAt()).isNotNull();
        assertThat(response.status()).isEqualTo("ANSWERED");

        verify(notificationService).createNotification(
                eq(requester), eq("OWNER_ANSWER_RECEIVED"), eq("oc-1"),
                eq("담당자 답변이 도착했습니다"),
                eq("김담당님이 질문에 답변했습니다."),
                eq("ws-1"));

        ArgumentCaptor<String> wsCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSocketSessionRegistry).sendToUser(eq("EMP001"), wsCaptor.capture());

        Map<String, Object> payload = objectMapper.readValue(wsCaptor.getValue(), Map.class);
        assertThat(payload).containsEntry("type", "owner_answer_received");
        assertThat(payload).containsEntry("confirmation_id", "oc-1");
        assertThat(payload).containsEntry("original_message_id", "msg-1");
        assertThat(payload).containsEntry("content", "답변 내용입니다.");
        assertThat(payload).containsEntry("owner_name", "김담당");
    }

    @Test
    void submitAnswer_배정되지않은담당자가시도시_IllegalStateException이발생한다() {
        User owner = createUser("owner-1", "EMP010", "김담당");
        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();

        OwnerConfirmation confirmation = OwnerConfirmation.builder()
                .workspace(workspace)
                .assignedOwner(owner)
                .build();
        ReflectionTestUtils.setField(confirmation, "id", "oc-1");

        when(ownerConfirmationRepository.findByIdAndDeletedAtIsNull("oc-1"))
                .thenReturn(Optional.of(confirmation));

        assertThatThrownBy(() -> ownerConfirmationService.submitAnswer("oc-1", "EMP999", "답변"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("권한이 없습니다. 배정된 담당자만 답변할 수 있습니다.");

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitAnswer_이미답변된요청시_IllegalStateException이발생한다() {
        User owner = createUser("owner-1", "EMP010", "김담당");
        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();

        OwnerConfirmation confirmation = OwnerConfirmation.builder()
                .workspace(workspace)
                .assignedOwner(owner)
                .build();
        ReflectionTestUtils.setField(confirmation, "id", "oc-1");
        confirmation.submitAnswer("이미 답변함");

        when(ownerConfirmationRepository.findByIdAndDeletedAtIsNull("oc-1"))
                .thenReturn(Optional.of(confirmation));

        assertThatThrownBy(() -> ownerConfirmationService.submitAnswer("oc-1", "EMP010", "새 답변"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 답변이 완료된 요청입니다.");
    }

    @Test
    void submitAnswer_requester가null인경우_알림없이답변만저장된다() {
        User owner = createUser("owner-1", "EMP010", "김담당");
        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();

        OwnerConfirmation confirmation = OwnerConfirmation.builder()
                .workspace(workspace)
                .assignedOwner(owner)
                .requester(null)
                .build();
        ReflectionTestUtils.setField(confirmation, "id", "oc-1");

        when(ownerConfirmationRepository.findByIdAndDeletedAtIsNull("oc-1"))
                .thenReturn(Optional.of(confirmation));

        ownerConfirmationService.submitAnswer("oc-1", "EMP010", "답변");

        assertThat(confirmation.getStatus()).isEqualTo("ANSWERED");
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
        verify(webSocketSessionRegistry, never()).sendToUser(anyString(), anyString());
    }

    // --- getDetail ---

    @Test
    void getDetail_assignedOwner가조회하면_정상반환된다() {
        User owner = createUser("owner-1", "EMP010", "김담당");
        User requester = createUser("requester-1", "EMP001", "홍질문");
        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();

        OwnerConfirmation confirmation = OwnerConfirmation.builder()
                .workspace(workspace)
                .assignedOwner(owner)
                .requester(requester)
                .questionContent("테스트 질문")
                .build();
        ReflectionTestUtils.setField(confirmation, "id", "oc-1");

        when(ownerConfirmationRepository.findByIdAndDeletedAtIsNull("oc-1"))
                .thenReturn(Optional.of(confirmation));

        OwnerConfirmationResponse response = ownerConfirmationService.getDetail("oc-1", "EMP010");

        assertThat(response.id()).isEqualTo("oc-1");
        assertThat(response.questionContent()).isEqualTo("테스트 질문");
    }

    @Test
    void getDetail_requester가조회하면_정상반환된다() {
        User owner = createUser("owner-1", "EMP010", "김담당");
        User requester = createUser("requester-1", "EMP001", "홍질문");
        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();

        OwnerConfirmation confirmation = OwnerConfirmation.builder()
                .workspace(workspace)
                .assignedOwner(owner)
                .requester(requester)
                .questionContent("테스트 질문")
                .build();
        ReflectionTestUtils.setField(confirmation, "id", "oc-1");

        when(ownerConfirmationRepository.findByIdAndDeletedAtIsNull("oc-1"))
                .thenReturn(Optional.of(confirmation));

        OwnerConfirmationResponse response = ownerConfirmationService.getDetail("oc-1", "EMP001");

        assertThat(response.id()).isEqualTo("oc-1");
    }

    @Test
    void getDetail_권한없는사용자시_IllegalStateException이발생한다() {
        User owner = createUser("owner-1", "EMP010", "김담당");
        User requester = createUser("requester-1", "EMP001", "홍질문");
        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();

        OwnerConfirmation confirmation = OwnerConfirmation.builder()
                .workspace(workspace)
                .assignedOwner(owner)
                .requester(requester)
                .build();
        ReflectionTestUtils.setField(confirmation, "id", "oc-1");

        when(ownerConfirmationRepository.findByIdAndDeletedAtIsNull("oc-1"))
                .thenReturn(Optional.of(confirmation));

        assertThatThrownBy(() -> ownerConfirmationService.getDetail("oc-1", "EMP999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("해당 확인 요청에 대한 접근 권한이 없습니다.");
    }

    private User createUser(String id, String employeeId, String name) {
        return User.builder()
                .id(id).employeeId(employeeId).name(name)
                .systemRole("USER").authProvider("LOCAL").build();
    }
}
