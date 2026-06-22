package com.devbridge.backend.domain.git.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitChangedFileDetail {

    private String filePath;
    private String changeType;
    private Integer additions;
    private Integer deletions;
    private String patch;
    private String diffSummary;
}