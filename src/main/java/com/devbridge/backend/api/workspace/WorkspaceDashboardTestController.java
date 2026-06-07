package com.devbridge.backend.api.workspace;

import com.devbridge.backend.domain.task.repository.TaskRepository;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WorkspaceDashboardTestController {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TaskRepository taskRepository;

    @GetMapping("/api/workspace/dashboard/test")
    public Map<String, Object> testWorkspaceDashboardDatabase() {
        return Map.of(
                "message", "workspace dashboard database connected",
                "userCount", userRepository.count(),
                "workspaceCount", workspaceRepository.count(),
                "taskCount", taskRepository.count()
        );
    }
}