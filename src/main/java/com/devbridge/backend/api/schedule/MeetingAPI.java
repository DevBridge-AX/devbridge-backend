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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @Operation(summary = "내 확정 일정 조회", description = "로그인한 사용자가 현재 워크스페이스에서 참여 중인 CONFIRMED 상태 회의의 확정 시간 슬롯을 기간 내에서 조회합니다. 캘린더의 선택 불가(Blocked) 영역 렌더링에 사용됩니다.")
    @GetMapping("/participants/me/schedules")
    ResponseEntity<List<ConfirmedScheduleResponse>> getMyConfirmedSchedules(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String employeeId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);

    @Operation(summary = "내 회의 목록 조회", description = "로그인한 사용자가 현재 워크스페이스에서 참여 중인 회의 목록을 조회합니다. 확정(CONFIRMED) 회의와 조율 중인 회의를 모두 포함하며, status 파라미터로 상태를 필터링할 수 있습니다.")
    @GetMapping
    ResponseEntity<List<MeetingSummaryResponse>> getMyMeetings(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String employeeId,
            @RequestParam(value = "status", required = false) MeetingStatus status);

    @Operation(summary = "회의 상세 조회", description = "회의의 확정 시간, 후보 시간, 참석자 목록, 첨부파일 목록, 상태 등 상세 정보를 조회합니다.")
    @GetMapping("/{meetingId}")
    ResponseEntity<MeetingDetailResponse> getMeetingDetail(
            @PathVariable("meetingId") String meetingId,
            @AuthenticationPrincipal String employeeId);

    @Operation(summary = "회의 정보 수정", description = "회의 주최자가 제목, 목적, 아젠다, 장소를 부분 수정합니다. 요청에 포함되지 않은(null) 필드는 기존 값이 유지되므로 필드를 하나씩 개별적으로 수정할 수 있습니다. 주최자가 아닌 경우 수정할 수 없으며, 수정 시 다른 참석자 전원에게 알림이 전달됩니다.")
    @PatchMapping("/{meetingId}")
    ResponseEntity<MeetingDetailResponse> updateMeeting(
            @PathVariable("meetingId") String meetingId,
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody UpdateMeetingRequest request);

    @Operation(summary = "회의 첨부파일 추가", description = "회의 상세 화면에서 파일 업로드 또는 링크 형태의 첨부파일을 추가합니다.")
    @PostMapping("/{meetingId}/references")
    ResponseEntity<MeetingReferenceResponse> addReference(
            @PathVariable("meetingId") String meetingId,
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody MeetingReferenceRequest request);

    @Operation(summary = "회의 첨부파일 삭제", description = "회의 상세 화면에서 첨부파일을 삭제합니다.")
    @DeleteMapping("/{meetingId}/references/{referenceId}")
    ResponseEntity<Void> deleteReference(
            @PathVariable("meetingId") String meetingId,
            @PathVariable("referenceId") String referenceId,
            @AuthenticationPrincipal String employeeId);
}
