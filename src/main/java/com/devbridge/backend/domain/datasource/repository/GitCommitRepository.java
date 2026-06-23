package com.devbridge.backend.domain.datasource.repository;

import com.devbridge.backend.domain.datasource.entity.GitCommit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GitCommitRepository extends JpaRepository<GitCommit, String> {

    List<GitCommit> findTop5ByDataSource_Workspace_IdOrderByPushedAtDesc(String workspaceId);

    List<GitCommit> findByWorkspace_IdOrderByPushedAtDesc(String workspaceId);

    Optional<GitCommit> findByWorkspace_IdAndCommitHash(String workspaceId, String commitHash);

    boolean existsByWorkspace_IdAndCommitHash(String workspaceId, String commitHash);
}