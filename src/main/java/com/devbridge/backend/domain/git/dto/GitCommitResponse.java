package com.devbridge.backend.domain.git.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitCommitResponse {

    private String hash;
    private String shortHash;
    private String authorName;
    private String authorEmail;
    private String committedAt;
    private String message;
    private String branchName;
}