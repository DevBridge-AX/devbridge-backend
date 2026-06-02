package com.devbridge.backend.domain.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateScheduleRequest {
    private String workspaceId;
    private String requesterId;
    private String title;
}
