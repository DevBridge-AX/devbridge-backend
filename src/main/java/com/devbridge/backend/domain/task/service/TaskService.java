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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final String STATUS_ASSIGNED = "ASSIGNED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_REVIEW = "REVIEW";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String LINK_TYPE_MANUAL = "MANUAL";

    private final TaskRepository taskRepository;
    private final WorkspaceContextValidator workspaceContextValidator;
    private final UserRepository userRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final TaskStatusLogRepository taskStatusLogRepository;
    private final TaskGitCommitRepository taskGitCommitRepository;
    private final GitCommitRepository gitCommitRepository;
    private final GitCommitAnalysisRepository gitCommitAnalysisRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByWorkspace(String workspaceId) {
        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);

        return taskRepository.findByWorkspace_Id(workspace.getId())
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskDetailResponse getTaskDetail(String taskId) {
        Task task = findTaskById(taskId);
        return toTaskDetailResponse(task);
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        validateCreateTaskRequest(request);

        Workspace workspace = workspaceContextValidator.getValidWorkspace(request.getWorkspaceId());

        User requester = userRepository.findById(request.getRequesterId())
                .orElseThrow(() -> new IllegalArgumentException("Requester not found: " + request.getRequesterId()));

        User assignee = findNullableUser(request.getAssigneeId());

        Task task = Task.builder()
                .workspace(workspace)
                .requester(requester)
                .assignee(assignee)
                .title(request.getTitle().trim())
                .description(normalizeNullableText(request.getDescription()))
                .status(STATUS_ASSIGNED)
                .dueDate(request.getDueDate())
                .build();

        Task savedTask = taskRepository.save(task);
        return toTaskResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(String taskId, UpdateTaskRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Update task request is required.");
        }

        Task task = findTaskById(taskId);
        User assignee = findNullableUser(request.getAssigneeId());

        String title = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle().trim()
                : task.getTitle();

        String description = request.getDescription() != null
                ? normalizeNullableText(request.getDescription())
                : task.getDescription();

        task.updateTask(
                assignee,
                title,
                description,
                request.getDueDate()
        );

        return toTaskResponse(task);
    }

    @Transactional
    public void updateTaskStatus(String taskId, UpdateTaskStatusRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("Task status is required.");
        }

        String nextStatus = request.getStatus().trim();
        validateStatus(nextStatus);

        Task task = findTaskById(taskId);
        String previousStatus = task.getStatus();

        User changedBy = findNullableUser(request.getChangedBy());
        if (changedBy == null) {
            changedBy = task.getRequester();
        }

        task.updateStatus(nextStatus);

        taskStatusLogRepository.save(
                TaskStatusLog.builder()
                        .task(task)
                        .status(nextStatus)
                        .previousStatus(previousStatus)
                        .nextStatus(nextStatus)
                        .changedBy(changedBy)
                        .build()
        );
    }

    @Transactional
    public TaskDetailResponse linkGitCommitToTask(String taskId, LinkTaskCommitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Link task commit request is required.");
        }

        if (request.getCommitId() == null || request.getCommitId().isBlank()) {
            throw new IllegalArgumentException("Commit ID is required.");
        }

        Task task = findTaskById(taskId);

        GitCommit gitCommit = gitCommitRepository.findById(request.getCommitId())
                .orElseThrow(() -> new IllegalArgumentException("Git commit not found: " + request.getCommitId()));

        workspaceContextValidator.validateSameWorkspace(
                task.getWorkspace().getId(),
                gitCommit.getWorkspace().getId(),
                "Git commit does not belong to the same workspace."
        );

        boolean alreadyLinked = taskGitCommitRepository.existsByTask_IdAndGitCommit_Id(
                task.getId(),
                gitCommit.getId()
        );

        if (!alreadyLinked) {
            User linkedBy = findNullableUser(request.getLinkedBy());
            if (linkedBy == null) {
                linkedBy = task.getRequester();
            }

            taskGitCommitRepository.save(
                    TaskGitCommit.builder()
                            .task(task)
                            .gitCommit(gitCommit)
                            .linkedBy(linkedBy)
                            .linkType(normalizeLinkType(request.getLinkType()))
                            .build()
            );
        }

        return toTaskDetailResponse(task);
    }

    @Transactional
    public void deleteTask(String taskId) {
        Task task = findTaskById(taskId);
        taskRepository.delete(task);
    }

    private Task findTaskById(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID is required.");
        }

        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    private User findNullableUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void validateCreateTaskRequest(CreateTaskRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create task request is required.");
        }

        if (request.getWorkspaceId() == null || request.getWorkspaceId().isBlank()) {
            throw new IllegalArgumentException("Workspace ID is required.");
        }

        if (request.getRequesterId() == null || request.getRequesterId().isBlank()) {
            throw new IllegalArgumentException("Requester ID is required.");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title is required.");
        }
    }

    private void validateStatus(String status) {
        if (!List.of(
                STATUS_ASSIGNED,
                STATUS_IN_PROGRESS,
                STATUS_REVIEW,
                STATUS_DONE,
                STATUS_CANCELLED
        ).contains(status)) {
            throw new IllegalArgumentException("Unsupported task status: " + status);
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeLinkType(String value) {
        if (value == null || value.isBlank()) {
            return LINK_TYPE_MANUAL;
        }

        return value.trim();
    }

    private TaskResponse toTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .workspaceId(task.getWorkspace().getId())
                .requesterId(task.getRequester().getId())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .build();
    }

    private TaskDetailResponse toTaskDetailResponse(Task task) {
        List<KnowledgeDocument> recentDocuments =
                knowledgeDocumentRepository.findTop5ByTask_IdOrderByCreatedAtDesc(task.getId());

        long documentCount = knowledgeDocumentRepository.countByTask_Id(task.getId());

        List<TaskStatusLog> recentActivities =
                taskStatusLogRepository.findTop5ByTask_IdOrderByChangedAtDesc(task.getId());

        long activityCount = taskStatusLogRepository.countByTask_Id(task.getId());

        List<TaskGitCommit> recentCommits =
                taskGitCommitRepository.findTop5ByTask_IdOrderByCreatedAtDesc(task.getId());

        long commitCount = taskGitCommitRepository.countByTask_Id(task.getId());

        return TaskDetailResponse.builder()
                .id(task.getId())
                .workspaceId(task.getWorkspace().getId())
                .workspaceName(task.getWorkspace().getName())
                .requesterId(task.getRequester().getId())
                .requesterName(task.getRequester().getName())
                .requesterDepartment(task.getRequester().getDepartment())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
                .assigneeDepartment(task.getAssignee() != null ? task.getAssignee().getDepartment() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(null)
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .aiSummary(buildTaskAiSummary(task, documentCount, commitCount))
                .progressSummary(buildProgressSummary(task, documentCount, commitCount))
                .nextAction(buildNextAction(task, documentCount, commitCount))
                .riskLevel(buildRiskLevel(task, documentCount, commitCount))
                .documentCount((int) documentCount)
                .commitCount((int) commitCount)
                .deliverableCount(0)
                .activityCount((int) activityCount)
                .recentDocuments(
                        recentDocuments.stream()
                                .map(this::toRelatedDocumentPreview)
                                .toList()
                )
                .recentCommits(
                        recentCommits.stream()
                                .map(this::toRelatedCommitPreview)
                                .toList()
                )
                .recentDeliverables(List.of())
                .recentActivities(
                        recentActivities.stream()
                                .map(this::toActivityPreview)
                                .toList()
                )
                .build();
    }

    private TaskDetailResponse.ActivityPreview toActivityPreview(TaskStatusLog log) {
        User changedBy = log.getChangedBy();

        String previousStatus = log.getPreviousStatus();
        String nextStatus = log.getNextStatus() != null ? log.getNextStatus() : log.getStatus();

        String message;
        if (previousStatus != null && !previousStatus.isBlank()) {
            message = "상태가 " + previousStatus + "에서 " + nextStatus + "(으)로 변경되었습니다.";
        } else {
            message = "상태가 " + nextStatus + "(으)로 변경되었습니다.";
        }

        return TaskDetailResponse.ActivityPreview.builder()
                .id(log.getId())
                .type("STATUS_CHANGE")
                .message(message)
                .actorName(changedBy != null ? changedBy.getName() : null)
                .createdAt(log.getChangedAt() != null ? log.getChangedAt() : log.getCreatedAt())
                .build();
    }

    private TaskDetailResponse.RelatedCommitPreview toRelatedCommitPreview(TaskGitCommit taskGitCommit) {
        GitCommit commit = taskGitCommit.getGitCommit();

        GitCommitAnalysis analysis = gitCommitAnalysisRepository.findByGitCommit_Id(commit.getId())
                .orElse(null);

        return TaskDetailResponse.RelatedCommitPreview.builder()
                .id(commit.getId())
                .commitHash(commit.getCommitHash())
                .shortHash(commit.getShortHash())
                .message(commit.getCommitMessage())
                .authorName(commit.getAuthorName())
                .authorEmail(commit.getAuthorEmail())
                .branchName(commit.getBranchName())
                .summary(analysis != null ? analysis.getSummary() : null)
                .impactArea(analysis != null ? analysis.getImpactArea() : null)
                .riskLevel(analysis != null ? analysis.getRiskLevel() : null)
                .nextAction(analysis != null ? analysis.getNextAction() : null)
                .indexStatus(analysis != null ? analysis.getIndexStatus() : null)
                .committedAt(commit.getPushedAt())
                .analyzedAt(analysis != null ? analysis.getAnalyzedAt() : null)
                .build();
    }

    private TaskDetailResponse.RelatedDocumentPreview toRelatedDocumentPreview(
            KnowledgeDocument document
    ) {
        return TaskDetailResponse.RelatedDocumentPreview.builder()
                .id(document.getId())
                .title(document.getTitle())
                .summary(document.getSummary())
                .keywords(document.getKeywords())
                .riskLevel(document.getRiskLevel())
                .nextAction(document.getNextAction())
                .analysisStatus(document.getAnalysisStatus())
                .analysisModel(document.getAnalysisModel())
                .analysisMode(document.getAnalysisMode())
                .analyzedAt(document.getAnalyzedAt())
                .uploadedAt(document.getCreatedAt())
                .build();
    }

    private String buildTaskAiSummary(Task task, long documentCount, long commitCount) {
        if (documentCount == 0 && commitCount == 0) {
            return "아직 이 업무에 연결된 문서나 Git 변경사항이 없습니다. 문서 또는 커밋을 연결하면 AI 분석 요약이 이 영역에 표시됩니다.";
        }

        return "이 업무에는 " + documentCount + "개의 문서와 " + commitCount + "개의 Git 변경사항이 연결되어 있습니다. 연결된 문서와 Git 분석 결과를 기반으로 업무 맥락을 확인할 수 있습니다.";
    }

    private String buildProgressSummary(Task task, long documentCount, long commitCount) {
        if (STATUS_DONE.equalsIgnoreCase(task.getStatus())) {
            return "업무가 완료된 상태입니다. 연결 문서와 Git 변경사항을 최종 확인하세요.";
        }

        if (STATUS_IN_PROGRESS.equalsIgnoreCase(task.getStatus())) {
            if (documentCount > 0 || commitCount > 0) {
                return "업무가 진행 중이며 연결 문서 또는 Git 변경사항이 등록되어 있습니다. 분석 결과와 최근 변경 이력을 함께 확인하세요.";
            }

            return "업무가 진행 중이지만 아직 연결 문서나 Git 변경사항이 없습니다. 담당자는 관련 자료를 연결해야 합니다.";
        }

        if (documentCount > 0 || commitCount > 0) {
            return "업무가 배정된 상태이며 연결 자료가 등록되어 있습니다. 문서와 Git 분석 결과를 검토하고 진행 상태를 갱신하세요.";
        }

        return "업무가 배정된 상태입니다. 필요한 문서와 Git 변경사항을 연결하고 작업을 시작하세요.";
    }

    private String buildNextAction(Task task, long documentCount, long commitCount) {
        if (STATUS_DONE.equalsIgnoreCase(task.getStatus())) {
            return "연결 문서와 Git 변경사항을 최종 검토하세요.";
        }

        if (documentCount == 0 && commitCount == 0) {
            return "업무 수행에 필요한 문서나 Git 변경사항을 연결하세요.";
        }

        if (STATUS_IN_PROGRESS.equalsIgnoreCase(task.getStatus())) {
            return "연결된 문서와 Git 분석 결과를 확인하고 다음 작업을 진행하세요.";
        }

        return "업무 상태를 진행 중으로 변경하고 연결 자료를 검토하세요.";
    }

    private String buildRiskLevel(Task task, long documentCount, long commitCount) {
        if (documentCount == 0 && commitCount == 0 && STATUS_IN_PROGRESS.equalsIgnoreCase(task.getStatus())) {
            return "MEDIUM";
        }

        return "LOW";
    }
}