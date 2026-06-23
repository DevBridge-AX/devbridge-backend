package com.devbridge.backend.domain.task.repository;

import com.devbridge.backend.domain.task.entity.TaskGitCommit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskGitCommitRepository extends JpaRepository<TaskGitCommit, String> {

    List<TaskGitCommit> findTop5ByTask_IdOrderByCreatedAtDesc(String taskId);

    long countByTask_Id(String taskId);

    boolean existsByTask_IdAndGitCommit_Id(String taskId, String commitId);
}