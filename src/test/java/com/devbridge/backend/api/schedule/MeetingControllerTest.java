package com.devbridge.backend.api.schedule;

import com.devbridge.backend.domain.schedule.dto.*;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import com.devbridge.backend.domain.schedule.entity.ParticipantRole;
import com.devbridge.backend.domain.schedule.entity.ParticipantStatus;
import com.devbridge.backend.domain.schedule.service.MeetingReferenceService;
import com.devbridge.backend.domain.schedule.service.MeetingService;
import com.devbridge.backend.global.auth.jwt.JwtTokenProvider;
import com.devbridge.backend.global.common.exception.BusinessException;
import com.devbridge.backend.global.common.exception.ErrorCode;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                "meeting-uuid-123", "스프린트 회고", "회고 진행", "지난 스프린트 회고", "회의실 A", null, 60, MeetingStatus.CONFIRMED,
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
        UpdateMeetingRequest request = new UpdateMeetingRequest("수정된 회의", "목적", "아젠다", "회의실 A", null);

        MeetingDetailResponse detailResponse = new MeetingDetailResponse(
                "meeting-uuid-123", "수정된 회의", "목적", "아젠다", "회의실 A", null, 60, MeetingStatus.GATHERING,
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
        UpdateMeetingRequest request = new UpdateMeetingRequest("수정된 회의", "목적", "아젠다", "회의실 A", null);

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

    @Test
    @DisplayName("9. 회의 취소 API 성공 테스트 (PATCH /api/meetings/{meetingId}/cancel)")
    void cancelMeeting_Success() throws Exception {
        MeetingDetailResponse detailResponse = new MeetingDetailResponse(
                "meeting-uuid-123", "스프린트 회고", null, null, null, null, 60, MeetingStatus.CANCELED,
                null, null, List.of(), List.of(), List.of());

        when(meetingService.cancelMeeting(any(), any())).thenReturn(detailResponse);

        mockMvc.perform(patch("/api/meetings/meeting-uuid-123/cancel")
                        .with(authentication(getMockAuthentication("EMP001")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("10. 회의 시간 수동 확정 API 성공 테스트 (POST /api/meetings/{meetingId}/confirm)")
    void confirmMeetingManually_Success() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 6, 20, 14, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 20, 15, 0);
        ManualConfirmRequest request = new ManualConfirmRequest(start, end);

        MeetingDetailResponse detailResponse = new MeetingDetailResponse(
                "meeting-uuid-123", "스프린트 회고", null, null, null, null, 60, MeetingStatus.CONFIRMED,
                start, end, List.of(), List.of(), List.of());

        when(meetingService.confirmMeetingManually(any(), any(), any())).thenReturn(detailResponse);

        mockMvc.perform(post("/api/meetings/meeting-uuid-123/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(getMockAuthentication("EMP001")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedStartTime").value("2026-06-20T14:00:00"));
    }

    @Test
    @DisplayName("11. 회의 재조율 요청 API 성공 테스트 (POST /api/meetings/{meetingId}/reopen)")
    void reopenMeeting_Success() throws Exception {
        MeetingDetailResponse detailResponse = new MeetingDetailResponse(
                "meeting-uuid-123", "스프린트 회고", null, null, null, null, 60, MeetingStatus.GATHERING,
                null, null, List.of(), List.of(), List.of());

        when(meetingService.reopenMeeting(any(), any())).thenReturn(detailResponse);

        mockMvc.perform(post("/api/meetings/meeting-uuid-123/reopen")
                        .with(authentication(getMockAuthentication("EMP001")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GATHERING"));
    }

    @Test
    @DisplayName("12. 회의 참석자 추가 API 성공 테스트 (POST /api/meetings/{meetingId}/participants)")
    void addParticipants_Success() throws Exception {
        AddParticipantsRequest request = new AddParticipantsRequest(List.of("EMP005"));

        MeetingParticipantResponse newParticipant = new MeetingParticipantResponse(
                "EMP005", "박민준", "기획팀", "PM", ParticipantRole.ATTENDEE, ParticipantStatus.PENDING);

        MeetingDetailResponse detailResponse = new MeetingDetailResponse(
                "meeting-uuid-123", "스프린트 회고", null, null, null, null, 60, MeetingStatus.GATHERING,
                null, null, List.of(), List.of(newParticipant), List.of());

        when(meetingService.addParticipants(any(), any(), any())).thenReturn(detailResponse);

        mockMvc.perform(post("/api/meetings/meeting-uuid-123/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(getMockAuthentication("EMP001")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0].employeeId").value("EMP005"));
    }

    @Test
    @DisplayName("13. 회의 참석자 제외 API 성공 테스트 (DELETE /api/meetings/{meetingId}/participants/{employeeId})")
    void removeParticipant_Success() throws Exception {
        doNothing().when(meetingService).removeParticipant(any(), any(), any());

        mockMvc.perform(delete("/api/meetings/meeting-uuid-123/participants/EMP005")
                        .with(authentication(getMockAuthentication("EMP001")))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("14. 회의 참석 거절 API 성공 테스트 (POST /api/meetings/{meetingId}/participants/me/decline)")
    void declineMeeting_Success() throws Exception {
        SubmitAvailableTimesResponse response = new SubmitAvailableTimesResponse(
                "meeting-uuid-123", "EMP002", ParticipantStatus.DECLINED, false);

        when(meetingService.declineMeeting(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/meetings/meeting-uuid-123/participants/me/decline")
                        .with(authentication(getMockAuthentication("EMP002")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    @DisplayName("15. [예외] 회의 참석 거절 API - 주최자가 거절을 시도하는 경우 예외 테스트")
    void declineMeeting_HostCannotDecline_Failure() throws Exception {
        when(meetingService.declineMeeting(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.SCHEDULE_HOST_CANNOT_DECLINE));

        mockMvc.perform(post("/api/meetings/meeting-uuid-123/participants/me/decline")
                        .with(authentication(getMockAuthentication("EMP001")))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
