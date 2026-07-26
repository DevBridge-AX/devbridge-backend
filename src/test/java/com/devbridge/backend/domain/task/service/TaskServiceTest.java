package com.devbridge.backend.domain.task.service;

import com.devbridge.backend.domain.datasource.entity.GitCommit;
import com.devbridge.backend.domain.datasource.entity.GitCommitAnalysis;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.GitCommitAnalysisRepository;
import com.devbridge.backend.domain.datasource.repository.GitCommitRepository;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import com.devbridge.backend.domain.task.dto.CreateTaskRequest;
import com.devbridge.backend.domain.task.dto.LinkTaskCommitRequest;
import com.devbridge.backend.domain.task.dto.TaskDetailResponse;
import com.devbridge.backend.domain.task.dto.TaskResponse;
import com.devbridge.backend.domain.task.dto.UpdateTaskRequest;
import com.devbridge.backend.domain.task.dto.UpdateTaskStatusRequest;
import com.devbridge.backend.domain.task.entity.Task;
import com.devbridge.backend.domain.task.entity.TaskGitCommit;
import com.devbridge.backend.domain.task.entity.TaskStatusLog;
import com.devbridge.backend.domain.task.repository.TaskGitCommitRepository;
import com.devbridge.backend.domain.task.repository.TaskRepository;
import com.devbridge.backend.domain.task.repository.TaskStatusLogRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.service.WorkspaceContextValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TaskService} 특성 테스트(characterization test).
 *
 * <p>"올바른 동작"이 아니라 <b>현재 동작</b>을 그대로 고정하는 것이 목적이다.
 * 이 클래스는 아래 두 리팩토링의 안전망으로 쓰인다.
 *
 * <ul>
 *   <li>도메인 경계 정리 — {@code UserRepository}·{@code KnowledgeDocumentRepository} 등
 *       타 도메인 Repository 직접 참조를 각 도메인 서비스 호출로 치환할 때,
 *       조회 실패·null 허용 동작이 동일하게 유지되는지 검증한다.</li>
 *   <li>예외 체계 도입 — 현재 던지는 {@link IllegalArgumentException}은
 *       {@code GlobalExceptionHandler}에서 <b>HTTP 400</b>으로 매핑된다.
 *       커스텀 예외로 치환할 때 상태코드가 바뀌면 프론트엔드가 조용히 깨지므로,
 *       예외 타입과 메시지를 여기서 고정한다.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final String WORKSPACE_ID = "WS-001";
    private static final String TASK_ID = "TASK-001";
    private static final String REQUESTER_ID = "USER-REQ";
    private static final String ASSIGNEE_ID = "USER-ASG";

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkspaceContextValidator workspaceContextValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private TaskStatusLogRepository taskStatusLogRepository;

    @Mock
    private TaskGitCommitRepository taskGitCommitRepository;

    @Mock
    private GitCommitRepository gitCommitRepository;

    @Mock
    private GitCommitAnalysisRepository gitCommitAnalysisRepository;

    private TaskService taskService;

    private Workspace workspace;
    private User requester;
    private User assignee;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository,
                workspaceContextValidator,
                userRepository,
                knowledgeDocumentRepository,
                taskStatusLogRepository,
                taskGitCommitRepository,
                gitCommitRepository,
                gitCommitAnalysisRepository
        );

        workspace = Workspace.builder().id(WORKSPACE_ID).name("테스트 워크스페이스").build();
        requester = User.builder().id(REQUESTER_ID).employeeId(REQUESTER_ID).name("요청자").department("기획").build();
        assignee = User.builder().id(ASSIGNEE_ID).employeeId(ASSIGNEE_ID).name("담당자").department("개발").build();
    }

    private Task task(String status) {
        return Task.builder()
                .id(TASK_ID)
                .workspace(workspace)
                .requester(requester)
                .title("기존 제목")
                .description("기존 설명")
                .status(status)
                .build();
    }

    /** {@code toTaskDetailResponse()}가 호출하는 6개 조회를 기본값(빈 목록/0건)으로 채운다. */
    private void stubDetailLookups(long documentCount, long commitCount) {
        lenient().when(knowledgeDocumentRepository.findTop5ByTask_IdOrderByCreatedAtDesc(TASK_ID))
                .thenReturn(List.of());
        lenient().when(knowledgeDocumentRepository.countByTask_Id(TASK_ID)).thenReturn(documentCount);
        lenient().when(taskStatusLogRepository.findTop5ByTask_IdOrderByChangedAtDesc(TASK_ID))
                .thenReturn(List.of());
        lenient().when(taskStatusLogRepository.countByTask_Id(TASK_ID)).thenReturn(0L);
        lenient().when(taskGitCommitRepository.findTop5ByTask_IdOrderByCreatedAtDesc(TASK_ID))
                .thenReturn(List.of());
        lenient().when(taskGitCommitRepository.countByTask_Id(TASK_ID)).thenReturn(commitCount);
    }

    @Nested
    @DisplayName("createTask")
    class CreateTask {

        @Test
        @DisplayName("createTask_withValidRequest_savesTaskWithAssignedStatusAndTrimmedTitle")
        void createTask_withValidRequest_savesTaskWithAssignedStatusAndTrimmedTitle() {
            when(workspaceContextValidator.getValidWorkspace(WORKSPACE_ID)).thenReturn(workspace);
            when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
            when(userRepository.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponse response = taskService.createTask(CreateTaskRequest.builder()
                    .workspaceId(WORKSPACE_ID)
                    .requesterId(REQUESTER_ID)
                    .assigneeId(ASSIGNEE_ID)
                    .title("  새 업무  ")
                    .description("   ")
                    .build());

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(captor.capture());

            assertThat(captor.getValue().getTitle()).isEqualTo("새 업무");
            // 공백만 있는 설명은 null로 정규화된다.
            assertThat(captor.getValue().getDescription()).isNull();
            assertThat(captor.getValue().getStatus()).isEqualTo("ASSIGNED");
            assertThat(response.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
            assertThat(response.getAssigneeId()).isEqualTo(ASSIGNEE_ID);
        }

        @Test
        @DisplayName("createTask_withBlankAssigneeId_savesTaskWithoutAssignee")
        void createTask_withBlankAssigneeId_savesTaskWithoutAssignee() {
            when(workspaceContextValidator.getValidWorkspace(WORKSPACE_ID)).thenReturn(workspace);
            when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponse response = taskService.createTask(CreateTaskRequest.builder()
                    .workspaceId(WORKSPACE_ID)
                    .requesterId(REQUESTER_ID)
                    .assigneeId("   ")
                    .title("담당자 없는 업무")
                    .build());

            assertThat(response.getAssigneeId()).isNull();
            // 담당자 ID가 공백이면 조회 자체를 하지 않는다.
            verify(userRepository, never()).findById("   ");
        }

        @Test
        @DisplayName("createTask_withNullRequest_throwsIllegalArgumentException")
        void createTask_withNullRequest_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> taskService.createTask(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Create task request is required.");
        }

        @Test
        @DisplayName("createTask_withBlankTitle_throwsIllegalArgumentException")
        void createTask_withBlankTitle_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> taskService.createTask(CreateTaskRequest.builder()
                    .workspaceId(WORKSPACE_ID)
                    .requesterId(REQUESTER_ID)
                    .title("  ")
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Task title is required.");
        }

        @Test
        @DisplayName("createTask_withUnknownRequester_throwsIllegalArgumentException")
        void createTask_withUnknownRequester_throwsIllegalArgumentException() {
            when(workspaceContextValidator.getValidWorkspace(WORKSPACE_ID)).thenReturn(workspace);
            when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(CreateTaskRequest.builder()
                    .workspaceId(WORKSPACE_ID)
                    .requesterId(REQUESTER_ID)
                    .title("새 업무")
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Requester not found: " + REQUESTER_ID);

            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateTask")
    class UpdateTask {

        @Test
        @DisplayName("updateTask_withBlankTitleAndNullDescription_keepsExistingValues")
        void updateTask_withBlankTitleAndNullDescription_keepsExistingValues() {
            Task existing = task("ASSIGNED");
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existing));

            TaskResponse response = taskService.updateTask(TASK_ID, UpdateTaskRequest.builder()
                    .title("   ")
                    .description(null)
                    .build());

            assertThat(response.getTitle()).isEqualTo("기존 제목");
            assertThat(response.getDescription()).isEqualTo("기존 설명");
            // 담당자 ID가 없으면 null로 덮어쓴다.
            assertThat(response.getAssigneeId()).isNull();
        }

        @Test
        @DisplayName("updateTask_withNullRequest_throwsIllegalArgumentException")
        void updateTask_withNullRequest_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> taskService.updateTask(TASK_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Update task request is required.");
        }

        @Test
        @DisplayName("updateTask_withUnknownAssignee_throwsIllegalArgumentException")
        void updateTask_withUnknownAssignee_throwsIllegalArgumentException() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("ASSIGNED")));
            when(userRepository.findById(ASSIGNEE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(TASK_ID, UpdateTaskRequest.builder()
                    .assigneeId(ASSIGNEE_ID)
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found: " + ASSIGNEE_ID);
        }
    }

    @Nested
    @DisplayName("updateTaskStatus")
    class UpdateTaskStatus {

        @Test
        @DisplayName("updateTaskStatus_withValidStatus_savesLogWithPreviousStatus")
        void updateTaskStatus_withValidStatus_savesLogWithPreviousStatus() {
            Task existing = task("ASSIGNED");
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(existing));
            when(userRepository.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));

            taskService.updateTaskStatus(TASK_ID, UpdateTaskStatusRequest.builder()
                    .status("  IN_PROGRESS  ")
                    .changedBy(ASSIGNEE_ID)
                    .build());

            ArgumentCaptor<TaskStatusLog> captor = ArgumentCaptor.forClass(TaskStatusLog.class);
            verify(taskStatusLogRepository).save(captor.capture());

            TaskStatusLog log = captor.getValue();
            assertThat(existing.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(log.getPreviousStatus()).isEqualTo("ASSIGNED");
            assertThat(log.getNextStatus()).isEqualTo("IN_PROGRESS");
            assertThat(log.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(log.getChangedBy()).isEqualTo(assignee);
        }

        @Test
        @DisplayName("updateTaskStatus_withoutChangedBy_fallsBackToRequester")
        void updateTaskStatus_withoutChangedBy_fallsBackToRequester() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("ASSIGNED")));

            taskService.updateTaskStatus(TASK_ID, UpdateTaskStatusRequest.builder()
                    .status("DONE")
                    .build());

            ArgumentCaptor<TaskStatusLog> captor = ArgumentCaptor.forClass(TaskStatusLog.class);
            verify(taskStatusLogRepository).save(captor.capture());

            assertThat(captor.getValue().getChangedBy()).isEqualTo(requester);
        }

        @Test
        @DisplayName("updateTaskStatus_withUnsupportedStatus_throwsIllegalArgumentException")
        void updateTaskStatus_withUnsupportedStatus_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> taskService.updateTaskStatus(TASK_ID, UpdateTaskStatusRequest.builder()
                    .status("ARCHIVED")
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unsupported task status: ARCHIVED");

            // 상태 검증은 업무 조회보다 먼저 수행된다.
            verify(taskRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("updateTaskStatus_withBlankStatus_throwsIllegalArgumentException")
        void updateTaskStatus_withBlankStatus_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> taskService.updateTaskStatus(TASK_ID, UpdateTaskStatusRequest.builder()
                    .status("   ")
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Task status is required.");
        }
    }

    @Nested
    @DisplayName("linkGitCommitToTask")
    class LinkGitCommitToTask {

        private GitCommit gitCommit() {
            return GitCommit.builder()
                    .id("COMMIT-001")
                    .workspace(workspace)
                    .commitHash("abcdef1234567890")
                    .shortHash("abcdef1")
                    .build();
        }

        @Test
        @DisplayName("linkGitCommitToTask_withBlankLinkType_savesWithManualLinkType")
        void linkGitCommitToTask_withBlankLinkType_savesWithManualLinkType() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("ASSIGNED")));
            when(gitCommitRepository.findById("COMMIT-001")).thenReturn(Optional.of(gitCommit()));
            when(taskGitCommitRepository.existsByTask_IdAndGitCommit_Id(TASK_ID, "COMMIT-001")).thenReturn(false);
            stubDetailLookups(0L, 0L);

            taskService.linkGitCommitToTask(TASK_ID, LinkTaskCommitRequest.builder()
                    .commitId("COMMIT-001")
                    .build());

            ArgumentCaptor<TaskGitCommit> captor = ArgumentCaptor.forClass(TaskGitCommit.class);
            verify(taskGitCommitRepository).save(captor.capture());

            assertThat(captor.getValue().getLinkType()).isEqualTo("MANUAL");
            // linkedBy가 없으면 요청자로 대체된다.
            assertThat(captor.getValue().getLinkedBy()).isEqualTo(requester);
        }

        @Test
        @DisplayName("linkGitCommitToTask_withAlreadyLinkedCommit_doesNotSaveDuplicate")
        void linkGitCommitToTask_withAlreadyLinkedCommit_doesNotSaveDuplicate() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("ASSIGNED")));
            when(gitCommitRepository.findById("COMMIT-001")).thenReturn(Optional.of(gitCommit()));
            when(taskGitCommitRepository.existsByTask_IdAndGitCommit_Id(TASK_ID, "COMMIT-001")).thenReturn(true);
            stubDetailLookups(0L, 1L);

            TaskDetailResponse response = taskService.linkGitCommitToTask(TASK_ID, LinkTaskCommitRequest.builder()
                    .commitId("COMMIT-001")
                    .build());

            verify(taskGitCommitRepository, never()).save(any());
            assertThat(response.getCommitCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("linkGitCommitToTask_withUnknownCommit_throwsIllegalArgumentException")
        void linkGitCommitToTask_withUnknownCommit_throwsIllegalArgumentException() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("ASSIGNED")));
            when(gitCommitRepository.findById("COMMIT-404")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.linkGitCommitToTask(TASK_ID, LinkTaskCommitRequest.builder()
                    .commitId("COMMIT-404")
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Git commit not found: COMMIT-404");
        }

        @Test
        @DisplayName("linkGitCommitToTask_withBlankCommitId_throwsIllegalArgumentException")
        void linkGitCommitToTask_withBlankCommitId_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> taskService.linkGitCommitToTask(TASK_ID, LinkTaskCommitRequest.builder()
                    .commitId("  ")
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Commit ID is required.");
        }
    }

    @Nested
    @DisplayName("조회 및 파생 필드")
    class Lookup {

        @Test
        @DisplayName("getTaskDetail_withBlankTaskId_throwsIllegalArgumentException")
        void getTaskDetail_withBlankTaskId_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> taskService.getTaskDetail("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Task ID is required.");
        }

        @Test
        @DisplayName("getTaskDetail_withUnknownTaskId_throwsIllegalArgumentException")
        void getTaskDetail_withUnknownTaskId_throwsIllegalArgumentException() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskDetail(TASK_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Task not found: " + TASK_ID);
        }

        @Test
        @DisplayName("getTaskDetail_withNoLinkedDataAndInProgress_returnsMediumRiskLevel")
        void getTaskDetail_withNoLinkedDataAndInProgress_returnsMediumRiskLevel() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("IN_PROGRESS")));
            stubDetailLookups(0L, 0L);

            TaskDetailResponse response = taskService.getTaskDetail(TASK_ID);

            assertThat(response.getRiskLevel()).isEqualTo("MEDIUM");
            assertThat(response.getDocumentCount()).isZero();
            assertThat(response.getCommitCount()).isZero();
            assertThat(response.getNextAction()).isEqualTo("업무 수행에 필요한 문서나 Git 변경사항을 연결하세요.");
        }

        @Test
        @DisplayName("getTaskDetail_withNoLinkedDataAndAssigned_returnsLowRiskLevel")
        void getTaskDetail_withNoLinkedDataAndAssigned_returnsLowRiskLevel() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("ASSIGNED")));
            stubDetailLookups(0L, 0L);

            TaskDetailResponse response = taskService.getTaskDetail(TASK_ID);

            assertThat(response.getRiskLevel()).isEqualTo("LOW");
        }

        @Test
        @DisplayName("getTaskDetail_withLinkedData_returnsCountsAndLowRiskLevel")
        void getTaskDetail_withLinkedData_returnsCountsAndLowRiskLevel() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("IN_PROGRESS")));
            stubDetailLookups(2L, 3L);

            TaskDetailResponse response = taskService.getTaskDetail(TASK_ID);

            assertThat(response.getRiskLevel()).isEqualTo("LOW");
            assertThat(response.getDocumentCount()).isEqualTo(2);
            assertThat(response.getCommitCount()).isEqualTo(3);
            // 현재 구현은 산출물을 항상 0으로 반환한다.
            assertThat(response.getDeliverableCount()).isZero();
            assertThat(response.getPriority()).isNull();
        }

        @Test
        @DisplayName("getTasksByWorkspace_withValidWorkspace_delegatesToWorkspaceValidator")
        void getTasksByWorkspace_withValidWorkspace_delegatesToWorkspaceValidator() {
            when(workspaceContextValidator.getValidWorkspace(WORKSPACE_ID)).thenReturn(workspace);
            when(taskRepository.findByWorkspace_Id(WORKSPACE_ID)).thenReturn(List.of(task("ASSIGNED")));

            List<TaskResponse> responses = taskService.getTasksByWorkspace(WORKSPACE_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.getFirst().getWorkspaceId()).isEqualTo(WORKSPACE_ID);
            verify(workspaceContextValidator).getValidWorkspace(WORKSPACE_ID);
        }

        @Test
        @DisplayName("deleteTask_withUnknownTaskId_throwsBeforeDelete")
        void deleteTask_withUnknownTaskId_throwsBeforeDelete() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.deleteTask(TASK_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Task not found: " + TASK_ID);

            verify(taskRepository, never()).delete(any());
        }
    }

    /**
     * 상세 응답의 목록 매핑 로직.
     *
     * <p>{@code toRelatedDocumentPreview}, {@code toRelatedCommitPreview}, {@code toActivityPreview}는
     * 목록이 비어 있지 않을 때만 실행되므로, 빈 목록만 사용하는 다른 테스트로는 검증되지 않는다.
     * God Class 분해 시 이 매핑들이 별도 컴포넌트로 떨어져 나갈 후보이므로 여기서 고정한다.
     */
    @Nested
    @DisplayName("상세 응답 매핑")
    class DetailMapping {

        private final LocalDateTime analyzedAt = LocalDateTime.of(2026, 7, 20, 10, 30);

        private GitCommit commit() {
            return GitCommit.builder()
                    .id("COMMIT-001")
                    .workspace(workspace)
                    .commitHash("abcdef1234567890")
                    .shortHash("abcdef1")
                    .commitMessage("feat: 기능 추가")
                    .authorName("작성자")
                    .authorEmail("author@devbridge.com")
                    .branchName("develop")
                    .build();
        }

        private void stubEmptyExcept(
                List<KnowledgeDocument> documents,
                List<TaskStatusLog> logs,
                List<TaskGitCommit> commits
        ) {
            when(knowledgeDocumentRepository.findTop5ByTask_IdOrderByCreatedAtDesc(TASK_ID)).thenReturn(documents);
            when(knowledgeDocumentRepository.countByTask_Id(TASK_ID)).thenReturn((long) documents.size());
            when(taskStatusLogRepository.findTop5ByTask_IdOrderByChangedAtDesc(TASK_ID)).thenReturn(logs);
            when(taskStatusLogRepository.countByTask_Id(TASK_ID)).thenReturn((long) logs.size());
            when(taskGitCommitRepository.findTop5ByTask_IdOrderByCreatedAtDesc(TASK_ID)).thenReturn(commits);
            when(taskGitCommitRepository.countByTask_Id(TASK_ID)).thenReturn((long) commits.size());
        }

        @Test
        @DisplayName("getTaskDetail_withLinkedDocument_mapsDocumentPreviewFields")
        void getTaskDetail_withLinkedDocument_mapsDocumentPreviewFields() {
            KnowledgeDocument document = KnowledgeDocument.builder()
                    .id("DOC-001")
                    .title("요구사항 정의서")
                    .summary("문서 요약")
                    .keywords("인증,권한")
                    .riskLevel("HIGH")
                    .nextAction("검토 필요")
                    .analysisStatus("COMPLETED")
                    .analysisModel("model-x")
                    .analysisMode("FULL")
                    .analyzedAt(analyzedAt)
                    .build();

            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("ASSIGNED")));
            stubEmptyExcept(List.of(document), List.of(), List.of());

            TaskDetailResponse response = taskService.getTaskDetail(TASK_ID);

            assertThat(response.getRecentDocuments()).hasSize(1);
            TaskDetailResponse.RelatedDocumentPreview preview = response.getRecentDocuments().getFirst();
            assertThat(preview.getId()).isEqualTo("DOC-001");
            assertThat(preview.getTitle()).isEqualTo("요구사항 정의서");
            assertThat(preview.getAnalysisStatus()).isEqualTo("COMPLETED");
            assertThat(preview.getRiskLevel()).isEqualTo("HIGH");
            assertThat(preview.getAnalyzedAt()).isEqualTo(analyzedAt);
        }

        @Test
        @DisplayName("getTaskDetail_withCommitHavingAnalysis_mapsAnalysisFields")
        void getTaskDetail_withCommitHavingAnalysis_mapsAnalysisFields() {
            GitCommit commit = commit();
            TaskGitCommit link = TaskGitCommit.builder().gitCommit(commit).linkType("MANUAL").build();

            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("ASSIGNED")));
            stubEmptyExcept(List.of(), List.of(), List.of(link));
            when(gitCommitAnalysisRepository.findByGitCommit_Id("COMMIT-001")).thenReturn(
                    Optional.of(GitCommitAnalysis.builder()
                            .summary("커밋 요약")
                            .impactArea("auth")
                            .riskLevel("MEDIUM")
                            .nextAction("리뷰 요청")
                            .indexStatus("INDEXED")
                            .analyzedAt(analyzedAt)
                            .build())
            );

            TaskDetailResponse response = taskService.getTaskDetail(TASK_ID);

            TaskDetailResponse.RelatedCommitPreview preview = response.getRecentCommits().getFirst();
            assertThat(preview.getShortHash()).isEqualTo("abcdef1");
            assertThat(preview.getMessage()).isEqualTo("feat: 기능 추가");
            assertThat(preview.getAuthorEmail()).isEqualTo("author@devbridge.com");
            assertThat(preview.getSummary()).isEqualTo("커밋 요약");
            assertThat(preview.getImpactArea()).isEqualTo("auth");
            assertThat(preview.getIndexStatus()).isEqualTo("INDEXED");
        }

        @Test
        @DisplayName("getTaskDetail_withCommitWithoutAnalysis_mapsAnalysisFieldsAsNull")
        void getTaskDetail_withCommitWithoutAnalysis_mapsAnalysisFieldsAsNull() {
            TaskGitCommit link = TaskGitCommit.builder().gitCommit(commit()).linkType("MANUAL").build();

            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("ASSIGNED")));
            stubEmptyExcept(List.of(), List.of(), List.of(link));
            when(gitCommitAnalysisRepository.findByGitCommit_Id("COMMIT-001")).thenReturn(Optional.empty());

            TaskDetailResponse response = taskService.getTaskDetail(TASK_ID);

            TaskDetailResponse.RelatedCommitPreview preview = response.getRecentCommits().getFirst();
            // 커밋 자체 정보는 유지되고 분석 파생 필드만 null이 된다.
            assertThat(preview.getCommitHash()).isEqualTo("abcdef1234567890");
            assertThat(preview.getSummary()).isNull();
            assertThat(preview.getImpactArea()).isNull();
            assertThat(preview.getRiskLevel()).isNull();
            assertThat(preview.getNextAction()).isNull();
            assertThat(preview.getIndexStatus()).isNull();
            assertThat(preview.getAnalyzedAt()).isNull();
        }

        @Test
        @DisplayName("getTaskDetail_withStatusLogHavingPreviousStatus_buildsTransitionMessage")
        void getTaskDetail_withStatusLogHavingPreviousStatus_buildsTransitionMessage() {
            TaskStatusLog log = TaskStatusLog.builder()
                    .id("LOG-001")
                    .previousStatus("ASSIGNED")
                    .nextStatus("IN_PROGRESS")
                    .status("IN_PROGRESS")
                    .changedBy(assignee)
                    .changedAt(analyzedAt)
                    .build();

            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("IN_PROGRESS")));
            stubEmptyExcept(List.of(), List.of(log), List.of());

            TaskDetailResponse response = taskService.getTaskDetail(TASK_ID);

            TaskDetailResponse.ActivityPreview preview = response.getRecentActivities().getFirst();
            assertThat(preview.getType()).isEqualTo("STATUS_CHANGE");
            assertThat(preview.getMessage()).isEqualTo("상태가 ASSIGNED에서 IN_PROGRESS(으)로 변경되었습니다.");
            assertThat(preview.getActorName()).isEqualTo("담당자");
            assertThat(preview.getCreatedAt()).isEqualTo(analyzedAt);
        }

        @Test
        @DisplayName("getTaskDetail_withStatusLogWithoutPreviousStatus_buildsInitialMessage")
        void getTaskDetail_withStatusLogWithoutPreviousStatus_buildsInitialMessage() {
            TaskStatusLog log = TaskStatusLog.builder()
                    .id("LOG-002")
                    .previousStatus(null)
                    .nextStatus(null)
                    .status("ASSIGNED")
                    .changedBy(null)
                    .changedAt(analyzedAt)
                    .build();

            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task("ASSIGNED")));
            stubEmptyExcept(List.of(), List.of(log), List.of());

            TaskDetailResponse response = taskService.getTaskDetail(TASK_ID);

            TaskDetailResponse.ActivityPreview preview = response.getRecentActivities().getFirst();
            // nextStatus가 없으면 status로 대체하고, 이전 상태가 없으면 전이 표현을 생략한다.
            assertThat(preview.getMessage()).isEqualTo("상태가 ASSIGNED(으)로 변경되었습니다.");
            assertThat(preview.getActorName()).isNull();
        }
    }
}
