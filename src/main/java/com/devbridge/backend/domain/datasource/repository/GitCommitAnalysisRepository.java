package com.devbridge.backend.domain.datasource.repository;

import com.devbridge.backend.domain.datasource.entity.GitCommitAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GitCommitAnalysisRepository extends JpaRepository<GitCommitAnalysis, String> {

    Optional<GitCommitAnalysis> findByGitCommit_Id(String commitId);
}