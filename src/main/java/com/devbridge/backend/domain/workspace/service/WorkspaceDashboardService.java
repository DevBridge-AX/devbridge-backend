package com.devbridge.backend.domain.workspace.service;

import com.devbridge.backend.domain.task.entity.Task;
import com.devbridge.backend.domain.task.repository.TaskRepository;
import com.devbridge.backend.domain.workspace.dto.WorkspaceDashboardSummaryResponse;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceDashboardService {

    private final WorkspaceRepository workspaceRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceDashboardSummaryResponse getSummary(String workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        List<Task> tasks = taskRepository.findByWorkspace_Id(workspaceId);

        long totalTaskCount = tasks.size();
        long assignedTaskCount = countByStatus(tasks, "ASSIGNED");
        long inProgressTaskCount = countByStatus(tasks, "IN_PROGRESS");
        long doneTaskCount = countByStatus(tasks, "DONE");
        long delayedTaskCount = countByStatus(tasks, "DELAYED");

        int progressRate = calculateProgressRate(totalTaskCount, doneTaskCount);
        long memberCount = workspaceMemberRepository.countByWorkspace_Id(workspaceId);

        return WorkspaceDashboardSummaryResponse.builder()
                .workspaceId(workspace.getId())
                .workspaceName(workspace.getName())
                .totalTaskCount(totalTaskCount)
                .assignedTaskCount(assignedTaskCount)
                .inProgressTaskCount(inProgressTaskCount)
                .doneTaskCount(doneTaskCount)
                .delayedTaskCount(delayedTaskCount)
                .progressRate(progressRate)
                .memberCount(memberCount)
                .build();
    }

    private long countByStatus(List<Task> tasks, String status) {
        return tasks.stream()
                .filter(task -> status.equals(task.getStatus()))
                .count();
    }

    private int calculateProgressRate(long totalTaskCount, long doneTaskCount) {
        if (totalTaskCount == 0) {
            return 0;
        }

        return (int) Math.round((doneTaskCount * 100.0) / totalTaskCount);
    }
}