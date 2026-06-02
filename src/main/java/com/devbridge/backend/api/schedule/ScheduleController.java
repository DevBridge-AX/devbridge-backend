package com.devbridge.backend.api.schedule;

import com.devbridge.backend.domain.schedule.dto.CreateScheduleRequest;
import com.devbridge.backend.domain.schedule.dto.SmartScheduleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
public class ScheduleController implements ScheduleAPI {

    @Override
    public ResponseEntity<SmartScheduleResponse> createSchedule(CreateScheduleRequest request) {
        SmartScheduleResponse response = SmartScheduleResponse.builder()
                .id("dummy-schedule-id")
                .workspaceId(request.getWorkspaceId())
                .requesterId(request.getRequesterId())
                .title(request.getTitle())
                .topCandidateTimes("[\"2026-06-03T10:00:00\", \"2026-06-03T14:00:00\"]")
                .status("GATHERING")
                .build();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SmartScheduleResponse> getSchedule(String id) {
        SmartScheduleResponse response = SmartScheduleResponse.builder()
                .id(id)
                .workspaceId("dummy-workspace-id")
                .requesterId("dummy-user-id")
                .title("주간 회의")
                .topCandidateTimes("[\"2026-06-03T10:00:00\", \"2026-06-03T14:00:00\"]")
                .confirmedTime(LocalDateTime.now().plusDays(1))
                .meetingLink("https://zoom.us/j/123456789")
                .status("CONFIRMED")
                .build();
        return ResponseEntity.ok(response);
    }
}
