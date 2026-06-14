package com.devbridge.backend.domain.datasource.repository;

import com.devbridge.backend.domain.datasource.entity.GitCommit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GitCommitRepository extends JpaRepository<GitCommit, String> {

    List<GitCommit> findTop5ByDataSource_Workspace_IdOrderByPushedAtDesc(String workspaceId);
}