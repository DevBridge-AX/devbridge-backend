package com.devbridge.backend.api.task;

import com.devbridge.backend.domain.task.dto.CreateTaskRequest;
import com.devbridge.backend.domain.task.dto.TaskResponse;
import com.devbridge.backend.domain.task.dto.UpdateTaskStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Task", description = "Task API")
@RequestMapping("/api/tasks")
public interface TaskAPI {

    @Operation(summary = "Task 목록 조회", description = "Workspace ID 기준으로 Task 목록을 조회합니다.")
    @GetMapping
    ResponseEntity<List<TaskResponse>> getTasksByWorkspace(@RequestParam("workspaceId") String workspaceId);

    @Operation(summary = "Task 생성", description = "Workspace 안에 새로운 Task를 생성합니다.")
    @PostMapping
    ResponseEntity<TaskResponse> createTask(@RequestBody CreateTaskRequest request);

    @Operation(summary = "Task 상태 업데이트", description = "Task 진행 상태를 업데이트합니다.")
    @PutMapping("/{id}/status")
    ResponseEntity<Void> updateTaskStatus(@PathVariable("id") String id, @RequestBody UpdateTaskStatusRequest request);
}