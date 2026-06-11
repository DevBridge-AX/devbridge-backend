package com.devbridge.backend.api.schedule;

import com.devbridge.backend.domain.schedule.dto.CreateMeetingRequest;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingResponse;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesRequest;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Meeting", description = "When2meet 기반 회의 일정 조율 API 명세")
@RequestMapping("/api/meetings")
public interface MeetingAPI {

    @Operation(summary = "회의 조율 요청 방 생성", description = "주최자가 제목, 소요 시간, 대상자 사번 리스트를 입력해 회의 조율 방을 생성합니다.")
    @PostMapping
    ResponseEntity<CreateMeetingResponse> createMeeting(@Valid @RequestBody CreateMeetingRequest request);

    @Operation(summary = "참석자 가능 시간 제출", description = "참석자가 자신의 가능한 시간대 목록을 제출합니다.")
    @PostMapping("/{meetingId}/participants/{employeeId}/times")
    ResponseEntity<SubmitAvailableTimesResponse> submitAvailableTimes(
            @PathVariable("meetingId") String meetingId,
            @PathVariable("employeeId") String employeeId,
            @Valid @RequestBody SubmitAvailableTimesRequest request);
}
