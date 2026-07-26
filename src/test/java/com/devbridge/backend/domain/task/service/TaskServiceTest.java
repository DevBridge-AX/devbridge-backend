package com.devbridge.backend.domain.task.service;

import com.devbridge.backend.domain.datasource.entity.GitCommit;
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
}
