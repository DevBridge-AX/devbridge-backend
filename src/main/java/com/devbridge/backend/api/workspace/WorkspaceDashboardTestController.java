package com.devbridge.backend.api.workspace;

import com.devbridge.backend.domain.task.repository.TaskRepository;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.dto.AiSummaryResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceDashboardDetailResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceDashboardSummaryResponse;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import com.devbridge.backend.domain.workspace.service.WorkspaceAiSummaryService;
import com.devbridge.backend.domain.workspace.service.WorkspaceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WorkspaceDashboardTestController {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceDashboardService workspaceDashboardService;
    private final WorkspaceAiSummaryService workspaceAiSummaryService;

    @GetMapping("/api/workspace/dashboard/test")
    public Map<String, Object> testWorkspaceDashboardDatabase() {
        return Map.of(
                "message", "workspace dashboard database connected",
                "userCount", userRepository.count(),
                "workspaceCount", workspaceRepository.count(),
                "taskCount", taskRepository.count()
        );
    }

    @GetMapping("/api/workspace/dashboard/summary")
    public WorkspaceDashboardSummaryResponse getWorkspaceDashboardSummary(
            @RequestParam String workspaceId
    ) {
        return workspaceDashboardService.getSummary(workspaceId);
    }

    @GetMapping("/api/workspace/dashboard/detail")
    public WorkspaceDashboardDetailResponse getWorkspaceDashboardDetail(
            @RequestParam String workspaceId
    ) {
        return workspaceDashboardService.getDetail(workspaceId);
    }

    @PostMapping("/api/workspace/dashboard/ai-summary")
    public Mono<AiSummaryResponse> getWorkspaceAiSummary(
            @RequestParam String workspaceId
    ) {
        return workspaceAiSummaryService.generateAiSummary(workspaceId);
    }
}