package com.devbridge.backend.domain.task.service;

import com.devbridge.backend.domain.task.dto.CreateTaskRequest;
import com.devbridge.backend.domain.task.dto.TaskDetailResponse;
import com.devbridge.backend.domain.task.dto.TaskResponse;
import com.devbridge.backend.domain.task.dto.UpdateTaskRequest;
import com.devbridge.backend.domain.task.dto.UpdateTaskStatusRequest;
import com.devbridge.backend.domain.task.entity.Task;
import com.devbridge.backend.domain.task.repository.TaskRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
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

    private final TaskRepository taskRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByWorkspace(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("Workspace ID is required.");
        }

        return taskRepository.findByWorkspace_Id(workspaceId)
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

        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + request.getWorkspaceId()));

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
        task.updateStatus(nextStatus);
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
                .aiSummary(null)
                .progressSummary(buildProgressSummary(task))
                .nextAction(buildNextAction(task))
                .riskLevel(buildRiskLevel(task))
                .documentCount(0)
                .commitCount(0)
                .deliverableCount(0)
                .activityCount(0)
                .recentDocuments(List.of())
                .recentCommits(List.of())
                .recentDeliverables(List.of())
                .recentActivities(List.of())
                .build();
    }

    private String buildProgressSummary(Task task) {
        if (STATUS_DONE.equalsIgnoreCase(task.getStatus())) {
            return "업무가 완료된 상태입니다.";
        }

        if (STATUS_IN_PROGRESS.equalsIgnoreCase(task.getStatus())) {
            return "업무가 진행 중입니다. 관련 문서와 변경 이력을 연결하면 진행 상황을 더 정확히 확인할 수 있습니다.";
        }

        return "업무가 배정된 상태입니다. 상세 문서, 산출물, Git 변경 이력을 연결해 진행 정보를 보강할 수 있습니다.";
    }

    private String buildNextAction(Task task) {
        if (STATUS_DONE.equalsIgnoreCase(task.getStatus())) {
            return "산출물과 관련 문서를 최종 확인하세요.";
        }

        if (STATUS_IN_PROGRESS.equalsIgnoreCase(task.getStatus())) {
            return "최근 작업 내역과 관련 문서를 확인하고 다음 작업을 이어가세요.";
        }

        return "업무 착수 전 필요한 문서와 요구사항을 확인하세요.";
    }

    private String buildRiskLevel(Task task) {
        if (STATUS_IN_PROGRESS.equalsIgnoreCase(task.getStatus())) {
            return "MEDIUM";
        }

        return "LOW";
    }
}