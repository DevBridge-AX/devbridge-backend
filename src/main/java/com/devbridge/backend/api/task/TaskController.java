package com.devbridge.backend.api.task;

import com.devbridge.backend.domain.task.dto.CreateTaskRequest;
import com.devbridge.backend.domain.task.dto.TaskDetailResponse;
import com.devbridge.backend.domain.task.dto.TaskResponse;
import com.devbridge.backend.domain.task.dto.UpdateTaskRequest;
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
    public ResponseEntity<TaskDetailResponse> getTaskDetail(String id) {
        return ResponseEntity.ok(taskService.getTaskDetail(id));
    }

    @Override
    public ResponseEntity<TaskResponse> createTask(CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.createTask(request));
    }

    @Override
    public ResponseEntity<TaskResponse> updateTask(String id, UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @Override
    public ResponseEntity<Void> updateTaskStatus(String id, UpdateTaskStatusRequest request) {
        taskService.updateTaskStatus(id, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteTask(String id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}