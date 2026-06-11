package com.devbridge.backend.api.schedule;

import com.devbridge.backend.domain.schedule.dto.CreateMeetingRequest;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingResponse;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesRequest;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesResponse;
import com.devbridge.backend.domain.schedule.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeetingController implements MeetingAPI {

    private final MeetingService meetingService;

    @Override
    public ResponseEntity<CreateMeetingResponse> createMeeting(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String hostEmployeeId,
            CreateMeetingRequest request) {
        return ResponseEntity.ok(meetingService.createMeeting(workspaceId, hostEmployeeId, request));
    }

    @Override
    public ResponseEntity<SubmitAvailableTimesResponse> submitAvailableTimes(
            String meetingId, String employeeId, SubmitAvailableTimesRequest request) {
        return ResponseEntity.ok(meetingService.submitAvailableTimes(meetingId, employeeId, request));
    }
}
