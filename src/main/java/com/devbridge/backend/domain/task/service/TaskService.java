package com.devbridge.backend.domain.task.service;

import com.devbridge.backend.domain.task.dto.TaskResponse;
import com.devbridge.backend.domain.task.entity.Task;
import com.devbridge.backend.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByWorkspace(String workspaceId) {
        return taskRepository.findByWorkspace_Id(workspaceId)
                .stream()
                .map(this::toTaskResponse)
                .toList();
    }

    private TaskResponse toTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .workspaceId(task.getWorkspace().getId())
                .requesterId(task.getRequester().getId())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .build();
    }
}