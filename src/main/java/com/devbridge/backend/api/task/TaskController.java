package com.devbridge.backend.api.task;

import com.devbridge.backend.domain.task.dto.CreateTaskRequest;
import com.devbridge.backend.domain.task.dto.TaskResponse;
import com.devbridge.backend.domain.task.dto.UpdateTaskStatusRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController implements TaskAPI {

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
