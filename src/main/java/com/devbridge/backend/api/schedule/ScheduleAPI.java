package com.devbridge.backend.api.schedule;

import com.devbridge.backend.domain.schedule.dto.CreateScheduleRequest;
import com.devbridge.backend.domain.schedule.dto.SmartScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Schedule", description = "회의 일정 조율 API 명세")
@RequestMapping("/api/schedules")
public interface ScheduleAPI {

    @Operation(summary = "스마트 일정 요청", description = "회의 주제 및 대상자들의 가능 시간 조율을 위해 일정을 생성합니다.")
    @PostMapping
    ResponseEntity<SmartScheduleResponse> createSchedule(@RequestBody CreateScheduleRequest request);

    @Operation(summary = "일정 상태 조회", description = "회의 확정 정보 및 추천 시간 후보를 포함한 일정 상태를 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<SmartScheduleResponse> getSchedule(@PathVariable("id") String id);
}
