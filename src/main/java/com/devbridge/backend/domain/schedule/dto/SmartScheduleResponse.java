package com.devbridge.backend.domain.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartScheduleResponse {
    private String id;
    private String workspaceId;
    private String requesterId;
    private String title;
    private String topCandidateTimes;
    private LocalDateTime confirmedTime;
    private String meetingLink;
    private String aiSummary;
    private String status;
}
