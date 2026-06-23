package com.devbridge.backend.api.task;

import com.devbridge.backend.domain.task.dto.CreateTaskRequest;
import com.devbridge.backend.domain.task.dto.LinkTaskCommitRequest;
import com.devbridge.backend.domain.task.dto.TaskDetailResponse;
import com.devbridge.backend.domain.task.dto.TaskResponse;
import com.devbridge.backend.domain.task.dto.UpdateTaskRequest;
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

    @Operation(summary = "Task 상세 조회", description = "Task ID 기준으로 Task 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<TaskDetailResponse> getTaskDetail(@PathVariable("id") String id);

    @Operation(summary = "Task 생성", description = "Workspace 안에 새로운 Task를 생성합니다.")
    @PostMapping
    ResponseEntity<TaskResponse> createTask(@RequestBody CreateTaskRequest request);

    @Operation(summary = "Task 수정", description = "Task의 제목, 설명, 담당자, 마감일을 수정합니다.")
    @PutMapping("/{id}")
    ResponseEntity<TaskResponse> updateTask(
            @PathVariable("id") String id,
            @RequestBody UpdateTaskRequest request
    );

    @Operation(summary = "Task 상태 업데이트", description = "Task 진행 상태를 업데이트합니다.")
    @PutMapping("/{id}/status")
    ResponseEntity<Void> updateTaskStatus(
            @PathVariable("id") String id,
            @RequestBody UpdateTaskStatusRequest request
    );

    @Operation(summary = "Task와 Git Commit 연결", description = "Task에 Git commit을 직접 연결합니다.")
    @PostMapping("/{id}/commits")
    ResponseEntity<TaskDetailResponse> linkGitCommitToTask(
            @PathVariable("id") String id,
            @RequestBody LinkTaskCommitRequest request
    );

    @Operation(summary = "Task 삭제", description = "Task를 삭제합니다. 실제 DB에서는 soft delete 처리됩니다.")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteTask(@PathVariable("id") String id);
}