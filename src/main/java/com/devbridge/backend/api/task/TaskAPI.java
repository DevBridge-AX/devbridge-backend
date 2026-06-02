package com.devbridge.backend.api.task;

import com.devbridge.backend.domain.task.dto.CreateTaskRequest;
import com.devbridge.backend.domain.task.dto.TaskResponse;
import com.devbridge.backend.domain.task.dto.UpdateTaskStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Task", description = "업무 관리 API 명세")
@RequestMapping("/api/tasks")
public interface TaskAPI {

    @Operation(summary = "업무 생성", description = "워크스페이스 내에 새로운 업무를 생성합니다.")
    @PostMapping
    ResponseEntity<TaskResponse> createTask(@RequestBody CreateTaskRequest request);

    @Operation(summary = "업무 상태 업데이트", description = "업무 진행 상태를 업데이트합니다.")
    @PutMapping("/{id}/status")
    ResponseEntity<Void> updateTaskStatus(@PathVariable("id") String id, @RequestBody UpdateTaskStatusRequest request);
}
