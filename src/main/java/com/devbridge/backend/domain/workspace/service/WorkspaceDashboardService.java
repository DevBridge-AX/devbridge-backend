package com.devbridge.backend.domain.workspace.service;

import com.devbridge.backend.domain.datasource.entity.GitCommit;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.GitCommitRepository;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import com.devbridge.backend.domain.task.entity.Task;
import com.devbridge.backend.domain.task.repository.TaskRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.workspace.dto.DashboardDocumentItemResponse;
import com.devbridge.backend.domain.workspace.dto.DashboardGitCommitItemResponse;
import com.devbridge.backend.domain.workspace.dto.DashboardTaskItemResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceDashboardDetailResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceDashboardSummaryResponse;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceDashboardService {

    private final WorkspaceContextValidator workspaceContextValidator;
    private final TaskRepository taskRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final GitCommitRepository gitCommitRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public WorkspaceDashboardSummaryResponse getSummary(String workspaceId) {
        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);
        String validWorkspaceId = workspace.getId();

        List<Task> tasks = taskRepository.findByWorkspace_Id(validWorkspaceId);

        long totalTaskCount = tasks.size();
        long assignedTaskCount = countByStatus(tasks, "ASSIGNED");
        long inProgressTaskCount = countByStatus(tasks, "IN_PROGRESS");
        long doneTaskCount = countByStatus(tasks, "DONE");
        long delayedTaskCount = countByStatus(tasks, "DELAYED");

        int progressRate = calculateProgressRate(totalTaskCount, doneTaskCount);
        long memberCount = workspaceMemberRepository.countByWorkspace_Id(validWorkspaceId);

        return WorkspaceDashboardSummaryResponse.builder()
                .workspaceId(validWorkspaceId)
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

    public WorkspaceDashboardDetailResponse getDetail(String workspaceId) {
        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);
        String validWorkspaceId = workspace.getId();

        List<DashboardTaskItemResponse> recentTasks = taskRepository
                .findTop5ByWorkspace_IdOrderByCreatedAtDesc(validWorkspaceId)
                .stream()
                .map(this::toTaskItemResponse)
                .toList();

        List<DashboardTaskItemResponse> delayedTasks = taskRepository
                .findTop5ByWorkspace_IdAndStatusOrderByDueDateAsc(validWorkspaceId, "DELAYED")
                .stream()
                .map(this::toTaskItemResponse)
                .toList();

        List<DashboardGitCommitItemResponse> recentGitCommits = gitCommitRepository
                .findByWorkspace_IdOrderByPushedAtDesc(validWorkspaceId)
                .stream()
                .limit(5)
                .map(this::toGitCommitItemResponse)
                .toList();

        List<DashboardDocumentItemResponse> recentDocuments = knowledgeDocumentRepository
                .findTop5ByDataSource_Workspace_IdOrderByCreatedAtDesc(validWorkspaceId)
                .stream()
                .map(this::toDocumentItemResponse)
                .toList();

        return WorkspaceDashboardDetailResponse.builder()
                .recentTasks(recentTasks)
                .delayedTasks(delayedTasks)
                .recentGitCommits(recentGitCommits)
                .recentDocuments(recentDocuments)
                .build();
    }

    private DashboardTaskItemResponse toTaskItemResponse(Task task) {
        return DashboardTaskItemResponse.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .status(task.getStatus())
                .assigneeName(getUserNameOrUnassigned(task.getAssignee()))
                .dueDate(task.getDueDate())
                .build();
    }

    private DashboardGitCommitItemResponse toGitCommitItemResponse(GitCommit gitCommit) {
        String authorName = gitCommit.getAuthor() != null
                ? gitCommit.getAuthor().getName()
                : gitCommit.getAuthorName();
        if (authorName == null || authorName.isBlank()) {
            authorName = "미지정";
        }

        return DashboardGitCommitItemResponse.builder()
                .commitId(gitCommit.getId())
                .commitHash(gitCommit.getCommitHash())
                .commitMessage(gitCommit.getCommitMessage())
                .authorName(authorName)
                .branchName(gitCommit.getBranchName())
                .pushedAt(gitCommit.getPushedAt())
                .build();
    }

    private DashboardDocumentItemResponse toDocumentItemResponse(KnowledgeDocument document) {
        return DashboardDocumentItemResponse.builder()
                .documentId(document.getId())
                .title(document.getTitle())
                .sourceName(document.getDataSource().getSourceName())
                .createdAt(document.getCreatedAt())
                .build();
    }

    private String getUserNameOrUnassigned(User user) {
        if (user == null) {
            return "미지정";
        }

        return user.getName();
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