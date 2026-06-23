package com.devbridge.backend.domain.task.repository;

import com.devbridge.backend.domain.task.entity.TaskStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskStatusLogRepository extends JpaRepository<TaskStatusLog, String> {

    List<TaskStatusLog> findTop5ByTask_IdOrderByChangedAtDesc(String taskId);

    long countByTask_Id(String taskId);
}