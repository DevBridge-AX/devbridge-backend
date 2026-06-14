package com.devbridge.backend.api.task;

import com.devbridge.backend.domain.task.dto.CreateTaskRequest;
import com.devbridge.backend.domain.task.dto.TaskResponse;
import com.devbridge.backend.domain.task.dto.UpdateTaskStatusRequest;
import com.devbridge.backend.domain.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TaskController implements TaskAPI {

    private final TaskService taskService;

    @Override
    public ResponseEntity<List<TaskResponse>> getTasksByWorkspace(String workspaceId) {
        return ResponseEntity.ok(taskService.getTasksByWorkspace(workspaceId));
    }

    @Override
    public ResponseEntity<TaskResponse> createTask(CreateTaskRequest request) {
        TaskResponse response = TaskResponse.builder()
                .id("dummy-task-id")
                .workspaceId(request.getWorkspaceId())
                .requesterId(request.getRequesterId())
                .assigneeId(request.getAssigneeId())
                .title(request.getTitle())
                .description(request.getDescription())
                .status("ASSIGNED")
                .dueDate(request.getDueDate())
                .build();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> updateTaskStatus(String id, UpdateTaskStatusRequest request) {
        return ResponseEntity.ok().build();
    }
}