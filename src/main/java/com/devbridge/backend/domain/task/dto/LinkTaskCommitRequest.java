package com.devbridge.backend.domain.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkTaskCommitRequest {

    private String commitId;

    private String linkedBy;

    private String linkType;
}