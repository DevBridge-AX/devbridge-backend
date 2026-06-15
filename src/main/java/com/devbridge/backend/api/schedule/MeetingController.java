package com.devbridge.backend.api.schedule;

import com.devbridge.backend.domain.schedule.dto.ConfirmedScheduleResponse;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingRequest;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingResponse;
import com.devbridge.backend.domain.schedule.dto.MeetingDetailResponse;
import com.devbridge.backend.domain.schedule.dto.MeetingReferenceRequest;
import com.devbridge.backend.domain.schedule.dto.MeetingReferenceResponse;
import com.devbridge.backend.domain.schedule.dto.MeetingSummaryResponse;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesRequest;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesResponse;
import com.devbridge.backend.domain.schedule.dto.UpdateMeetingRequest;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import com.devbridge.backend.domain.schedule.service.MeetingReferenceService;
import com.devbridge.backend.domain.schedule.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MeetingController implements MeetingAPI {

    private final MeetingService meetingService;
    private final MeetingReferenceService meetingReferenceService;

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

    @Override
    public ResponseEntity<List<ConfirmedScheduleResponse>> getMyConfirmedSchedules(
            String workspaceId, String employeeId, LocalDate startDate, LocalDate endDate) {
        return ResponseEntity.ok(meetingService.getMyConfirmedSchedules(workspaceId, employeeId, startDate, endDate));
    }

    @Override
    public ResponseEntity<List<MeetingSummaryResponse>> getMyMeetings(String workspaceId, String employeeId, MeetingStatus status) {
        return ResponseEntity.ok(meetingService.getMyMeetings(workspaceId, employeeId, status));
    }

    @Override
    public ResponseEntity<MeetingDetailResponse> getMeetingDetail(String meetingId, String employeeId) {
        return ResponseEntity.ok(meetingService.getMeetingDetail(meetingId, employeeId));
    }

    @Override
    public ResponseEntity<MeetingDetailResponse> updateMeeting(String meetingId, String employeeId, UpdateMeetingRequest request) {
        return ResponseEntity.ok(meetingService.updateMeeting(meetingId, employeeId, request));
    }

    @Override
    public ResponseEntity<MeetingReferenceResponse> addReference(
            String meetingId, String employeeId, MeetingReferenceRequest request) {
        return ResponseEntity.ok(meetingReferenceService.addReference(meetingId, employeeId, request));
    }

    @Override
    public ResponseEntity<Void> deleteReference(String meetingId, String referenceId, String employeeId) {
        meetingReferenceService.deleteReference(meetingId, referenceId, employeeId);
        return ResponseEntity.noContent().build();
    }
}
