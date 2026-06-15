package com.devbridge.backend.domain.task.service;

import com.devbridge.backend.domain.task.dto.TaskDetailResponse;
import com.devbridge.backend.domain.task.dto.TaskResponse;
import com.devbridge.backend.domain.task.entity.Task;
import com.devbridge.backend.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByWorkspace(String workspaceId) {
        return taskRepository.findByWorkspace_Id(workspaceId)
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskDetailResponse getTaskDetail(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        return toTaskDetailResponse(task);
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
        if ("DONE".equalsIgnoreCase(task.getStatus())) {
            return "업무가 완료된 상태입니다.";
        }

        if ("IN_PROGRESS".equalsIgnoreCase(task.getStatus())) {
            return "업무가 진행 중입니다. 관련 문서와 변경 이력을 연결하면 진행 상황을 더 정확히 확인할 수 있습니다.";
        }

        if ("OVERDUE".equalsIgnoreCase(task.getStatus())) {
            return "마감 지연 가능성이 있는 업무입니다. 담당자 확인과 최근 변경 사항 검토가 필요합니다.";
        }

        return "업무가 배정된 상태입니다. 상세 문서, 산출물, Git 변경 이력을 연결해 진행 정보를 보강할 수 있습니다.";
    }

    private String buildNextAction(Task task) {
        if ("DONE".equalsIgnoreCase(task.getStatus())) {
            return "산출물과 관련 문서를 최종 확인하세요.";
        }

        if ("IN_PROGRESS".equalsIgnoreCase(task.getStatus())) {
            return "최근 작업 내역과 관련 문서를 확인하고 다음 작업을 이어가세요.";
        }

        if ("OVERDUE".equalsIgnoreCase(task.getStatus())) {
            return "마감일, 담당자, 남은 작업 범위를 우선 재검토하세요.";
        }

        return "업무 착수 전 필요한 문서와 요구사항을 확인하세요.";
    }

    private String buildRiskLevel(Task task) {
        if ("OVERDUE".equalsIgnoreCase(task.getStatus())) {
            return "HIGH";
        }

        if ("IN_PROGRESS".equalsIgnoreCase(task.getStatus())) {
            return "MEDIUM";
        }

        return "LOW";
    }
}