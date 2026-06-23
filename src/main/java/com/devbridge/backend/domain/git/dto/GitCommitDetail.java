package com.devbridge.backend.domain.git.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitCommitDetail {

    private String hash;
    private String shortHash;
    private String authorName;
    private String authorEmail;
    private LocalDateTime committedAt;
    private String message;
    private String branchName;
    private List<GitChangedFileDetail> changedFiles;
}