package com.devbridge.backend.api.schedule;

import com.devbridge.backend.domain.schedule.dto.ConfirmedScheduleResponse;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingRequest;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingResponse;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesRequest;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Meeting", description = "When2meet 기반 회의 일정 조율 API 명세")
@RequestMapping("/api/meetings")
public interface MeetingAPI {

    @Operation(summary = "회의 조율 요청 방 생성", description = "주최자가 제목, 소요 시간, 대상자 사번 리스트를 입력해 회의 조율 방을 생성합니다.")
    @PostMapping
    ResponseEntity<CreateMeetingResponse> createMeeting(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String hostEmployeeId,
            @Valid @RequestBody CreateMeetingRequest request);

    @Operation(summary = "참석자 가능 시간 제출", description = "로그인한 참석자가 자신의 가능한 시간대 목록을 제출합니다.")
    @PostMapping("/{meetingId}/participants/me/times")
    ResponseEntity<SubmitAvailableTimesResponse> submitAvailableTimes(
            @PathVariable("meetingId") String meetingId,
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody SubmitAvailableTimesRequest request);

    @Operation(summary = "내 확정 일정 조회", description = "로그인한 사용자가 참여 중인 CONFIRMED 상태 회의의 확정 시간 슬롯을 기간 내에서 조회합니다. 캘린더의 선택 불가(Blocked) 영역 렌더링에 사용됩니다.")
    @GetMapping("/participants/me/schedules")
    ResponseEntity<List<ConfirmedScheduleResponse>> getMyConfirmedSchedules(
            @AuthenticationPrincipal String employeeId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);
}
