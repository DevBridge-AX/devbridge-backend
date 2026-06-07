package com.devbridge.backend.domain.task.repository;

import com.devbridge.backend.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {

    List<Task> findByWorkspace_Id(String workspaceId);
}