package com.devbridge.backend.api.schedule;

import com.devbridge.backend.domain.schedule.dto.*;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import com.devbridge.backend.domain.schedule.entity.ParticipantRole;
import com.devbridge.backend.domain.schedule.entity.ParticipantStatus;
import com.devbridge.backend.domain.schedule.service.MeetingReferenceService;
import com.devbridge.backend.domain.schedule.service.MeetingService;
import com.devbridge.backend.global.auth.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private MeetingService meetingService;

    @MockitoBean
    private MeetingReferenceService meetingReferenceService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private UsernamePasswordAuthenticationToken getMockAuthentication(String employeeId) {
        return new UsernamePasswordAuthenticationToken(employeeId, "mock-token", List.of());
    }

    @Test
    @DisplayName("1. 회의 조율 요청 방 생성 API 성공 테스트 (POST /api/meetings)")
    void createMeeting_Success() throws Exception {
        // given
        CreateMeetingRequest request = new CreateMeetingRequest("스프린트 회고", "회고 진행", "지난 스프린트 회고", "회의실 A", 60, List.of("EMP002", "EMP003"), null);
        CreateMeetingResponse response = new CreateMeetingResponse("meeting-uuid-123");

        when(meetingService.createMeeting(any(), any(), any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/meetings")
                        .header("X-Workspace-Id", "workspace-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(getMockAuthentication("EMP001")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingId").value("meeting-uuid-123"));
    }

    @Test
    @DisplayName("2. 참석자 가능 시간 제출 API 성공 테스트 (POST /api/meetings/{meetingId}/participants/me/times)")
    void submitAvailableTimes_Success() throws Exception {
        // given
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 15, 12, 0);
        TimeSlotRequest timeSlot = new TimeSlotRequest(startTime, endTime);
        SubmitAvailableTimesRequest request = new SubmitAvailableTimesRequest(List.of(timeSlot));
        
        SubmitAvailableTimesResponse response = new SubmitAvailableTimesResponse(
                "meeting-uuid-123", "EMP002", ParticipantStatus.RESPONDED, true);

        when(meetingService.submitAvailableTimes(any(), any(), any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/meetings/meeting-uuid-123/participants/me/times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(getMockAuthentication("EMP002")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingId").value("meeting-uuid-123"))
                .andExpect(jsonPath("$.employeeId").value("EMP002"))
                .andExpect(jsonPath("$.status").value("RESPONDED"))
                .andExpect(jsonPath("$.allParticipantsResponded").value(true));
    }

    @Test
    @DisplayName("3. 내 확정 일정 조회 API 성공 테스트 (GET /api/meetings/participants/me/schedules)")
    void getMyConfirmedSchedules_Success() throws Exception {
        // given
        ConfirmedScheduleResponse confirmed = new ConfirmedScheduleResponse(
                "meeting-uuid-123", "스프린트 회고", 
                LocalDateTime.of(2026, 6, 15, 10, 0), 
                LocalDateTime.of(2026, 6, 15, 11, 0));

        when(meetingService.getMyConfirmedSchedules(any(), any(), any(), any()))
                .thenReturn(List.of(confirmed));

        // when & then
        mockMvc.perform(get("/api/meetings/participants/me/schedules")
                        .header("X-Workspace-Id", "workspace-1")
                        .param("startDate", "2026-06-15")
                        .param("endDate", "2026-06-21")
                        .with(authentication(getMockAuthentication("EMP002"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].meetingId").value("meeting-uuid-123"))
                .andExpect(jsonPath("$[0].title").value("스프린트 회고"))
                .andExpect(jsonPath("$[0].confirmedStartTime").value("2026-06-15T10:00:00"))
                .andExpect(jsonPath("$[0].confirmedEndTime").value("2026-06-15T11:00:00"));
    }

    @Test
    @DisplayName("4. 내 회의 목록 조회 API 성공 테스트 (GET /api/meetings)")
    void getMyMeetings_Success() throws Exception {
        // given
        MeetingSummaryResponse summary = new MeetingSummaryResponse(
                "meeting-uuid-123", "스프린트 회고", MeetingStatus.GATHERING, 60, null, null);

        when(meetingService.getMyMeetings(any(), any(), any()))
                .thenReturn(List.of(summary));

        // when & then
        mockMvc.perform(get("/api/meetings")
                        .header("X-Workspace-Id", "workspace-1")
                        .param("status", "GATHERING")
                        .with(authentication(getMockAuthentication("EMP002"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].meetingId").value("meeting-uuid-123"))
                .andExpect(jsonPath("$[0].title").value("스프린트 회고"))
                .andExpect(jsonPath("$[0].status").value("GATHERING"));
    }

    @Test
    @DisplayName("5. 회의 상세 조회 API 성공 테스트 (GET /api/meetings/{meetingId})")
    void getMeetingDetail_Success() throws Exception {
        // given
        MeetingParticipantResponse participant1 = new MeetingParticipantResponse("EMP001", "김현수", "개발팀", "Backend Developer", ParticipantRole.HOST, ParticipantStatus.RESPONDED);
        MeetingParticipantResponse participant2 = new MeetingParticipantResponse("EMP002", "홍길동", "디자인팀", "디자이너", ParticipantRole.ATTENDEE, ParticipantStatus.RESPONDED);
        CandidateTimeSlot candidate = new CandidateTimeSlot(
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 12, 0));

        MeetingDetailResponse detailResponse = new MeetingDetailResponse(
                "meeting-uuid-123", "스프린트 회고", "회고 진행", "지난 스프린트 회고", "회의실 A", 60, MeetingStatus.CONFIRMED,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 11, 0),
                List.of(candidate),
                List.of(participant1, participant2),
                List.of());

        when(meetingService.getMeetingDetail(any(), any()))
                .thenReturn(detailResponse);

        // when & then
        mockMvc.perform(get("/api/meetings/meeting-uuid-123")
                        .with(authentication(getMockAuthentication("EMP002"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingId").value("meeting-uuid-123"))
                .andExpect(jsonPath("$.title").value("스프린트 회고"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.participants[0].employeeId").value("EMP001"))
                .andExpect(jsonPath("$.topCandidateTimes[0].startTime").value("2026-06-15T10:00:00"));
    }

    @Test
    @DisplayName("6. [예외] 회의 생성 API - 비어있는 회의 제목 시 Validation 실패 테스트")
    void createMeeting_ValidationFailure() throws Exception {
        // given
        CreateMeetingRequest invalidRequest = new CreateMeetingRequest("", null, null, null, 60, List.of("EMP002"), null);

        // when & then
        mockMvc.perform(post("/api/meetings")
                        .header("X-Workspace-Id", "workspace-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(authentication(getMockAuthentication("EMP001")))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. 회의 정보 수정 API 성공 테스트 (PATCH /api/meetings/{meetingId})")
    void updateMeeting_Success() throws Exception {
        // given
        UpdateMeetingRequest request = new UpdateMeetingRequest("수정된 회의", "목적", "아젠다", "회의실 A");

        MeetingDetailResponse detailResponse = new MeetingDetailResponse(
                "meeting-uuid-123", "수정된 회의", "목적", "아젠다", "회의실 A", 60, MeetingStatus.GATHERING,
                null, null, List.of(), List.of(), List.of());

        when(meetingService.updateMeeting(any(), any(), any()))
                .thenReturn(detailResponse);

        // when & then
        mockMvc.perform(patch("/api/meetings/meeting-uuid-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(getMockAuthentication("EMP001")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 회의"))
                .andExpect(jsonPath("$.purpose").value("목적"))
                .andExpect(jsonPath("$.agenda").value("아젠다"))
                .andExpect(jsonPath("$.location").value("회의실 A"));
    }

    @Test
    @DisplayName("8. [예외] 회의 정보 수정 API - 주최자가 아닌 경우 예외 테스트")
    void updateMeeting_NotHost_Failure() throws Exception {
        // given
        UpdateMeetingRequest request = new UpdateMeetingRequest("수정된 회의", "목적", "아젠다", "회의실 A");

        when(meetingService.updateMeeting(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("회의 주최자만 회의 정보를 수정할 수 있습니다."));

        // when & then
        mockMvc.perform(patch("/api/meetings/meeting-uuid-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(getMockAuthentication("EMP002")))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
