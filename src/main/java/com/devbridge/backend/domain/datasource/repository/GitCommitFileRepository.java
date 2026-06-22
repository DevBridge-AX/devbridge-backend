package com.devbridge.backend.domain.datasource.repository;

import com.devbridge.backend.domain.datasource.entity.GitCommitFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GitCommitFileRepository extends JpaRepository<GitCommitFile, String> {

    List<GitCommitFile> findByGitCommit_IdOrderByFilePathAsc(String commitId);
}