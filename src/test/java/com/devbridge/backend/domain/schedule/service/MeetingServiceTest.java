package com.devbridge.backend.domain.schedule.service;

import com.devbridge.backend.domain.schedule.dto.AddParticipantsRequest;
import com.devbridge.backend.domain.schedule.dto.CandidateTimeSlot;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingRequest;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingResponse;
import com.devbridge.backend.domain.schedule.dto.ManualConfirmRequest;
import com.devbridge.backend.domain.schedule.dto.MeetingDetailResponse;
import com.devbridge.backend.domain.schedule.dto.MeetingSummaryResponse;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesRequest;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesResponse;
import com.devbridge.backend.domain.schedule.dto.TimeSlotRequest;
import com.devbridge.backend.domain.schedule.dto.UpdateMeetingRequest;
import com.devbridge.backend.domain.schedule.entity.Meeting;
import com.devbridge.backend.domain.schedule.entity.MeetingParticipant;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import com.devbridge.backend.domain.schedule.entity.ParticipantAvailableTime;
import com.devbridge.backend.domain.schedule.entity.ParticipantRole;
import com.devbridge.backend.domain.schedule.entity.ParticipantStatus;
import com.devbridge.backend.domain.schedule.repository.MeetingParticipantRepository;
import com.devbridge.backend.domain.schedule.repository.MeetingRepository;
import com.devbridge.backend.domain.schedule.repository.ParticipantAvailableTimeRepository;
import com.devbridge.backend.domain.notification.service.NotificationService;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.service.UserInternalService;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.service.WorkspaceContextValidator;
import com.devbridge.backend.domain.workspace.service.WorkspaceService;
import com.devbridge.backend.global.common.exception.BusinessException;
import com.devbridge.backend.global.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;

    @Mock
    private ParticipantAvailableTimeRepository participantAvailableTimeRepository;

    @Mock
    private WorkspaceContextValidator workspaceContextValidator;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private MeetingReferenceService meetingReferenceService;

    @Mock
    private UserInternalService userInternalService;

    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private MeetingService meetingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        meetingService = new MeetingService(
                meetingRepository, meetingParticipantRepository, participantAvailableTimeRepository, workspaceContextValidator, workspaceService, meetingReferenceService, objectMapper, userInternalService, notificationService);

        lenient().when(userInternalService.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Optional.of(User.builder().id(id).employeeId(id).name("Mock User").systemRole("USER").authProvider("LOCAL").build());
        });
        lenient().when(userInternalService.findByEmployeeId(anyString())).thenAnswer(invocation -> {
            String empId = invocation.getArgument(0);
            return Optional.of(User.builder().id(empId).employeeId(empId).name("Mock User").systemRole("USER").authProvider("LOCAL").build());
        });
        lenient().when(workspaceContextValidator.getValidWorkspace(anyString())).thenAnswer(invocation -> {
            String wsId = invocation.getArgument(0);
            return Workspace.builder().id(wsId).name("Mock Workspace").build();
        });
    }

    @Test
    void submitAvailableTimes_미응답참석자가남아있으면_GATHERING상태를유지한다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant respondingAttendee = MeetingParticipant.builder()
                .id("participant-attendee-1")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        MeetingParticipant pendingAttendee = MeetingParticipant.builder()
                .id("participant-attendee-2")
                .meeting(meeting)
                .employeeId("EMP003")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP002"))
                .thenReturn(Optional.of(respondingAttendee));
        when(meetingParticipantRepository.findByMeetingId("meeting-1"))
                .thenReturn(List.of(host, respondingAttendee, pendingAttendee));

        SubmitAvailableTimesRequest request = new SubmitAvailableTimesRequest(
                List.of(new TimeSlotRequest(LocalDateTime.of(2026, 6, 15, 9, 0), LocalDateTime.of(2026, 6, 15, 11, 0))));

        SubmitAvailableTimesResponse response = meetingService.submitAvailableTimes("meeting-1", "EMP002", request);

        assertThat(response.allParticipantsResponded()).isFalse();
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.GATHERING);
        assertThat(meeting.getConfirmedStartTime()).isNull();
    }

    @Test
    void submitAvailableTimes_전원응답완료_교집합이소요시간이상이면_CONFIRMED로전환된다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-2")
                .workspace(ws)
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-2", "EMP002"))
                .thenReturn(Optional.of(attendee));
        when(meetingParticipantRepository.findByMeetingId("meeting-2"))
                .thenReturn(List.of(host, attendee));

        LocalDateTime rangeStart = LocalDateTime.of(2026, 6, 15, 9, 0);
        LocalDateTime rangeEnd = LocalDateTime.of(2026, 6, 15, 11, 0);

        when(participantAvailableTimeRepository.findByMeetingParticipant_MeetingId("meeting-2"))
                .thenReturn(List.of(
                        ParticipantAvailableTime.builder()
                                .id("time-host")
                                .meetingParticipant(host)
                                .startTime(rangeStart)
                                .endTime(rangeEnd)
                                .build(),
                        ParticipantAvailableTime.builder()
                                .id("time-attendee")
                                .meetingParticipant(attendee)
                                .startTime(rangeStart)
                                .endTime(rangeEnd)
                                .build()));

        SubmitAvailableTimesRequest request = new SubmitAvailableTimesRequest(
                List.of(new TimeSlotRequest(rangeStart, rangeEnd)));

        SubmitAvailableTimesResponse response = meetingService.submitAvailableTimes("meeting-2", "EMP002", request);

        assertThat(response.allParticipantsResponded()).isTrue();
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CONFIRMED);
        assertThat(meeting.getConfirmedStartTime()).isEqualTo(rangeStart);
        assertThat(meeting.getConfirmedEndTime()).isEqualTo(rangeStart.plusMinutes(60));
    }

    @Test
    void submitAvailableTimes_교집합이소요시간보다짧으면_SELECTING상태를유지한다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-3")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-3", "EMP002"))
                .thenReturn(Optional.of(attendee));
        when(meetingParticipantRepository.findByMeetingId("meeting-3"))
                .thenReturn(List.of(host, attendee));

        LocalDateTime rangeStart = LocalDateTime.of(2026, 6, 15, 9, 0);
        LocalDateTime rangeEnd = LocalDateTime.of(2026, 6, 15, 9, 30);

        when(participantAvailableTimeRepository.findByMeetingParticipant_MeetingId("meeting-3"))
                .thenReturn(List.of(
                        ParticipantAvailableTime.builder()
                                .id("time-host")
                                .meetingParticipant(host)
                                .startTime(rangeStart)
                                .endTime(rangeEnd)
                                .build(),
                        ParticipantAvailableTime.builder()
                                .id("time-attendee")
                                .meetingParticipant(attendee)
                                .startTime(rangeStart)
                                .endTime(rangeEnd)
                                .build()));

        SubmitAvailableTimesRequest request = new SubmitAvailableTimesRequest(
                List.of(new TimeSlotRequest(rangeStart, rangeEnd)));

        SubmitAvailableTimesResponse response = meetingService.submitAvailableTimes("meeting-3", "EMP002", request);

        assertThat(response.allParticipantsResponded()).isTrue();
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.SELECTING);
        assertThat(meeting.getConfirmedStartTime()).isNull();
        assertThat(meeting.getConfirmedEndTime()).isNull();
        assertThat(meeting.getTopCandidateTimes()).isEqualTo("[]");
    }

    @Test
    void submitAvailableTimes_전원응답완료_자동확정되면_참석자전원에게알림이발송된다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-4")
                .workspace(ws)
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-4", "EMP002"))
                .thenReturn(Optional.of(attendee));
        when(meetingParticipantRepository.findByMeetingId("meeting-4"))
                .thenReturn(List.of(host, attendee));

        LocalDateTime rangeStart = LocalDateTime.of(2026, 6, 15, 9, 0);
        LocalDateTime rangeEnd = LocalDateTime.of(2026, 6, 15, 11, 0);

        when(participantAvailableTimeRepository.findByMeetingParticipant_MeetingId("meeting-4"))
                .thenReturn(List.of(
                        ParticipantAvailableTime.builder()
                                .id("time-host")
                                .meetingParticipant(host)
                                .startTime(rangeStart)
                                .endTime(rangeEnd)
                                .build(),
                        ParticipantAvailableTime.builder()
                                .id("time-attendee")
                                .meetingParticipant(attendee)
                                .startTime(rangeStart)
                                .endTime(rangeEnd)
                                .build()));

        SubmitAvailableTimesRequest request = new SubmitAvailableTimesRequest(
                List.of(new TimeSlotRequest(rangeStart, rangeEnd)));

        meetingService.submitAvailableTimes("meeting-4", "EMP002", request);

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CONFIRMED);
        verify(notificationService, times(2)).createNotification(any(User.class), eq("MEETING_CONFIRMED"),
                eq("meeting-4"), eq("회의 일정이 확정되었습니다"),
                eq("제출된 가능 시간을 기반으로 회의 일정이 자동으로 확정되었습니다."), any());
    }

    @Test
    void submitAvailableTimes_교집합이부족해SELECTING상태로남으면_알림을보내지않는다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-5")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-5", "EMP002"))
                .thenReturn(Optional.of(attendee));
        when(meetingParticipantRepository.findByMeetingId("meeting-5"))
                .thenReturn(List.of(host, attendee));

        LocalDateTime rangeStart = LocalDateTime.of(2026, 6, 15, 9, 0);
        LocalDateTime rangeEnd = LocalDateTime.of(2026, 6, 15, 9, 30);

        when(participantAvailableTimeRepository.findByMeetingParticipant_MeetingId("meeting-5"))
                .thenReturn(List.of(
                        ParticipantAvailableTime.builder()
                                .id("time-host")
                                .meetingParticipant(host)
                                .startTime(rangeStart)
                                .endTime(rangeEnd)
                                .build(),
                        ParticipantAvailableTime.builder()
                                .id("time-attendee")
                                .meetingParticipant(attendee)
                                .startTime(rangeStart)
                                .endTime(rangeEnd)
                                .build()));

        SubmitAvailableTimesRequest request = new SubmitAvailableTimesRequest(
                List.of(new TimeSlotRequest(rangeStart, rangeEnd)));

        meetingService.submitAvailableTimes("meeting-5", "EMP002", request);

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.SELECTING);
        verify(notificationService, never()).createNotification(any(), eq("MEETING_CONFIRMED"), any(), any(), any(), any());
    }

    @Test
    void getMyMeetings_status가null이면_참여중인모든회의를최신순으로반환한다() {
        Meeting olderMeeting = Meeting.builder()
                .id("meeting-old")
                .title("지난 회의")
                .durationMinutes(30)
                .status(MeetingStatus.CONFIRMED)
                .build();
        ReflectionTestUtils.setField(olderMeeting, "createdAt", LocalDateTime.of(2026, 6, 10, 10, 0));

        Meeting newerMeeting = Meeting.builder()
                .id("meeting-new")
                .title("최근 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();
        ReflectionTestUtils.setField(newerMeeting, "createdAt", LocalDateTime.of(2026, 6, 11, 10, 0));

        MeetingParticipant participantOnOlder = MeetingParticipant.builder()
                .id("participant-old")
                .meeting(olderMeeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant participantOnNewer = MeetingParticipant.builder()
                .id("participant-new")
                .meeting(newerMeeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByEmployeeIdAndMeeting_Workspace_Id("EMP001", "workspace-1"))
                .thenReturn(List.of(participantOnOlder, participantOnNewer));

        List<MeetingSummaryResponse> result = meetingService.getMyMeetings("workspace-1", "EMP001", null);

        assertThat(result).extracting(MeetingSummaryResponse::meetingId)
                .containsExactly("meeting-new", "meeting-old");
        assertThat(result.get(1).status()).isEqualTo(MeetingStatus.CONFIRMED);
    }

    @Test
    void getMyMeetings_status가주어지면_해당상태의회의만조회한다() {
        when(meetingParticipantRepository.findByEmployeeIdAndMeeting_StatusAndMeeting_Workspace_Id("EMP001", MeetingStatus.CONFIRMED, "workspace-1"))
                .thenReturn(List.of());

        List<MeetingSummaryResponse> result = meetingService.getMyMeetings("workspace-1", "EMP001", MeetingStatus.CONFIRMED);

        assertThat(result).isEmpty();
        verify(meetingParticipantRepository).findByEmployeeIdAndMeeting_StatusAndMeeting_Workspace_Id("EMP001", MeetingStatus.CONFIRMED, "workspace-1");
    }

    @Test
    void getMeetingDetail_참석자인경우_확정정보와참석자목록을포함한상세정보를반환한다() throws Exception {
        LocalDateTime confirmedStart = LocalDateTime.of(2026, 6, 15, 9, 0);
        LocalDateTime confirmedEnd = LocalDateTime.of(2026, 6, 15, 10, 0);
        String topCandidateTimesJson = objectMapper.writeValueAsString(
                List.of(new CandidateTimeSlot(confirmedStart, confirmedEnd)));

        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.CONFIRMED)
                .confirmedStartTime(confirmedStart)
                .confirmedEndTime(confirmedEnd)
                .topCandidateTimes(topCandidateTimesJson)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP002"))
                .thenReturn(Optional.of(attendee));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host, attendee));
        when(meetingReferenceService.getReferences("meeting-1")).thenReturn(List.of());

        MeetingDetailResponse response = meetingService.getMeetingDetail("meeting-1", "EMP002");

        assertThat(response.meetingId()).isEqualTo("meeting-1");
        assertThat(response.status()).isEqualTo(MeetingStatus.CONFIRMED);
        assertThat(response.confirmedStartTime()).isEqualTo(confirmedStart);
        assertThat(response.confirmedEndTime()).isEqualTo(confirmedEnd);
        assertThat(response.topCandidateTimes()).containsExactly(new CandidateTimeSlot(confirmedStart, confirmedEnd));
        assertThat(response.participants()).extracting("employeeId")
                .containsExactlyInAnyOrder("EMP001", "EMP002");
        assertThat(response.references()).isEmpty();
    }

    @Test
    void getMeetingDetail_참석자가아니면_예외가발생한다() {
        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.getMeetingDetail("meeting-1", "EMP999"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 회의의 참석자가 아닙니다.")
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_NOT_PARTICIPANT));
    }

    @Test
    void updateMeeting_Host가요청하면_회의정보가수정되고_다른참석자에게알림이전송된다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .workspace(ws)
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host, attendee));
        when(meetingReferenceService.getReferences("meeting-1")).thenReturn(List.of());

        UpdateMeetingRequest request = new UpdateMeetingRequest("수정된 회의", "목적", "아젠다", "회의실 A", null);

        MeetingDetailResponse response = meetingService.updateMeeting("meeting-1", "EMP001", request);

        assertThat(meeting.getTitle()).isEqualTo("수정된 회의");
        assertThat(meeting.getPurpose()).isEqualTo("목적");
        assertThat(meeting.getAgenda()).isEqualTo("아젠다");
        assertThat(meeting.getLocation()).isEqualTo("회의실 A");
        assertThat(response.title()).isEqualTo("수정된 회의");
        assertThat(response.purpose()).isEqualTo("목적");
        assertThat(response.agenda()).isEqualTo("아젠다");
        assertThat(response.location()).isEqualTo("회의실 A");

        verify(notificationService).createNotification(any(User.class), eq("MEETING_UPDATED"), eq("meeting-1"),
                eq("회의 일정이 변경되었습니다"), eq("참여 중인 회의의 상세 정보가 변경되었습니다."), any());
    }

    @Test
    void updateMeeting_Host가아니면_예외가발생하고_알림도전송되지않는다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP002"))
                .thenReturn(Optional.of(attendee));

        UpdateMeetingRequest request = new UpdateMeetingRequest("수정된 회의", "목적", "아젠다", "회의실 A", null);

        assertThatThrownBy(() -> meetingService.updateMeeting("meeting-1", "EMP002", request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("회의 주최자만 회의 정보를 수정할 수 있습니다.")
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_NOT_HOST));

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateMeeting_필드를null로보내면_해당필드는기존값이유지된다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .workspace(ws)
                .title("주간 회의")
                .purpose("기존 목적")
                .agenda("기존 아젠다")
                .location("기존 장소")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host));
        when(meetingReferenceService.getReferences("meeting-1")).thenReturn(List.of());

        UpdateMeetingRequest request = new UpdateMeetingRequest(null, null, null, "새 장소", null);

        MeetingDetailResponse response = meetingService.updateMeeting("meeting-1", "EMP001", request);

        assertThat(meeting.getTitle()).isEqualTo("주간 회의");
        assertThat(meeting.getPurpose()).isEqualTo("기존 목적");
        assertThat(meeting.getAgenda()).isEqualTo("기존 아젠다");
        assertThat(meeting.getLocation()).isEqualTo("새 장소");
        assertThat(response.location()).isEqualTo("새 장소");
    }

    @Test
    void updateMeeting_meetingLink만변경하면_다른필드는유지되고_meetingLink만반영된다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .workspace(ws)
                .title("주간 회의")
                .purpose("기존 목적")
                .agenda("기존 아젠다")
                .location("기존 장소")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host));
        when(meetingReferenceService.getReferences("meeting-1")).thenReturn(List.of());

        UpdateMeetingRequest request = new UpdateMeetingRequest(null, null, null, null, "https://meet.example.com/room-1");

        MeetingDetailResponse response = meetingService.updateMeeting("meeting-1", "EMP001", request);

        assertThat(meeting.getLocation()).isEqualTo("기존 장소");
        assertThat(meeting.getMeetingLink()).isEqualTo("https://meet.example.com/room-1");
        assertThat(response.meetingLink()).isEqualTo("https://meet.example.com/room-1");
    }

    @Test
    void updateMeeting_title이빈값이면_예외가발생한다() {
        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));

        UpdateMeetingRequest request = new UpdateMeetingRequest("  ", null, null, null, null);

        assertThatThrownBy(() -> meetingService.updateMeeting("meeting-1", "EMP001", request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("회의 제목은 빈 값일 수 없습니다.")
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_INVALID_TITLE));

        verify(meetingRepository, never()).findById(any());
    }

    @Test
    void cancelMeeting_Host가요청하면_회의가취소되고_참석자에게알림이전송된다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .workspace(ws)
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host, attendee));
        when(meetingReferenceService.getReferences("meeting-1")).thenReturn(List.of());

        MeetingDetailResponse response = meetingService.cancelMeeting("meeting-1", "EMP001");

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CANCELED);
        assertThat(response.status()).isEqualTo(MeetingStatus.CANCELED);
        verify(notificationService).createNotification(any(User.class), eq("MEETING_CANCELED"), eq("meeting-1"),
                eq("회의가 취소되었습니다"), eq("참여 중인 회의가 주최자에 의해 취소되었습니다."), any());
    }

    @Test
    void cancelMeeting_이미취소된회의면_예외가발생한다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.CANCELED)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> meetingService.cancelMeeting("meeting-1", "EMP001"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_ALREADY_CANCELED));
    }

    @Test
    void cancelMeeting_Host가아니면_예외가발생한다() {
        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP002"))
                .thenReturn(Optional.of(attendee));

        assertThatThrownBy(() -> meetingService.cancelMeeting("meeting-1", "EMP002"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_NOT_HOST));

        verify(meetingRepository, never()).findById(any());
    }

    @Test
    void confirmMeetingManually_Host가요청하면_직접지정한시간으로확정되고_알림이전송된다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .workspace(ws)
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.SELECTING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host, attendee));
        when(meetingReferenceService.getReferences("meeting-1")).thenReturn(List.of());

        LocalDateTime start = LocalDateTime.of(2026, 6, 20, 14, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 20, 15, 0);
        ManualConfirmRequest request = new ManualConfirmRequest(start, end);

        MeetingDetailResponse response = meetingService.confirmMeetingManually("meeting-1", "EMP001", request);

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CONFIRMED);
        assertThat(response.confirmedStartTime()).isEqualTo(start);
        assertThat(response.confirmedEndTime()).isEqualTo(end);
        verify(notificationService).createNotification(any(User.class), eq("MEETING_CONFIRMED"), eq("meeting-1"),
                eq("회의 일정이 확정되었습니다"), eq("주최자가 회의 시간을 직접 확정했습니다."), any());
    }

    @Test
    void confirmMeetingManually_종료시간이시작시간보다빠르면_예외가발생한다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.SELECTING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));

        LocalDateTime start = LocalDateTime.of(2026, 6, 20, 15, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 20, 14, 0);
        ManualConfirmRequest request = new ManualConfirmRequest(start, end);

        assertThatThrownBy(() -> meetingService.confirmMeetingManually("meeting-1", "EMP001", request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_INVALID_TIME_RANGE));
    }

    @Test
    void confirmMeetingManually_이미확정된회의면_예외가발생한다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.CONFIRMED)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));

        ManualConfirmRequest request = new ManualConfirmRequest(
                LocalDateTime.of(2026, 6, 20, 14, 0), LocalDateTime.of(2026, 6, 20, 15, 0));

        assertThatThrownBy(() -> meetingService.confirmMeetingManually("meeting-1", "EMP001", request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_ALREADY_CONFIRMED));
    }

    @Test
    void reopenMeeting_SELECTING상태면_GATHERING으로되돌아가고_기존제출시간이초기화된다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .workspace(ws)
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.SELECTING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.ATTENDEE)
                .build();

        ParticipantAvailableTime existingTime = ParticipantAvailableTime.builder()
                .id("time-1")
                .meetingParticipant(attendee)
                .startTime(LocalDateTime.of(2026, 6, 15, 9, 0))
                .endTime(LocalDateTime.of(2026, 6, 15, 10, 0))
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));
        when(participantAvailableTimeRepository.findByMeetingParticipant_MeetingId("meeting-1"))
                .thenReturn(List.of(existingTime));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host, attendee));
        when(meetingReferenceService.getReferences("meeting-1")).thenReturn(List.of());

        MeetingDetailResponse response = meetingService.reopenMeeting("meeting-1", "EMP001");

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.GATHERING);
        assertThat(response.status()).isEqualTo(MeetingStatus.GATHERING);
        assertThat(host.getStatus()).isEqualTo(ParticipantStatus.PENDING);
        assertThat(attendee.getStatus()).isEqualTo(ParticipantStatus.PENDING);
        verify(participantAvailableTimeRepository).deleteAll(List.of(existingTime));
        verify(notificationService).createNotification(any(User.class), eq("MEETING_REOPENED"), eq("meeting-1"),
                eq("회의 일정 재조율이 요청되었습니다"), eq("가능한 시간을 다시 제출해주세요."), any());
    }

    @Test
    void reopenMeeting_SELECTING상태가아니면_예외가발생한다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> meetingService.reopenMeeting("meeting-1", "EMP001"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_INVALID_STATUS_FOR_REOPEN));

        verify(participantAvailableTimeRepository, never()).deleteAll(any());
    }

    @Test
    void addParticipants_Host가요청하면_참석자가추가되고_초대알림이전송된다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .workspace(ws)
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP005"))
                .thenReturn(Optional.empty());
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host));
        when(meetingReferenceService.getReferences("meeting-1")).thenReturn(List.of());

        AddParticipantsRequest request = new AddParticipantsRequest(List.of("EMP005"));

        meetingService.addParticipants("meeting-1", "EMP001", request);

        verify(meetingParticipantRepository).saveAll(argThat(list -> {
            List<MeetingParticipant> participants = (List<MeetingParticipant>) list;
            return participants.size() == 1
                    && participants.get(0).getEmployeeId().equals("EMP005")
                    && participants.get(0).getRole() == ParticipantRole.ATTENDEE
                    && participants.get(0).getStatus() == ParticipantStatus.PENDING;
        }));
        verify(notificationService).createNotification(any(User.class), eq("MEETING_INVITED"), eq("meeting-1"),
                eq("회의에 초대되었습니다"), eq("새 회의에 참석자로 초대되었습니다."), any());
    }

    @Test
    void addParticipants_이미참석자인경우_예외가발생한다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant existingAttendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP002"))
                .thenReturn(Optional.of(existingAttendee));
        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));

        AddParticipantsRequest request = new AddParticipantsRequest(List.of("EMP002"));

        assertThatThrownBy(() -> meetingService.addParticipants("meeting-1", "EMP001", request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_PARTICIPANT_ALREADY_EXISTS));

        verify(meetingParticipantRepository, never()).saveAll(any());
    }

    @Test
    void removeParticipant_Host가요청하면_참석자가제외된다() {
        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP002"))
                .thenReturn(Optional.of(attendee));

        meetingService.removeParticipant("meeting-1", "EMP001", "EMP002");

        verify(meetingParticipantRepository).delete(attendee);
    }

    @Test
    void removeParticipant_대상이Host면_예외가발생한다() {
        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));

        assertThatThrownBy(() -> meetingService.removeParticipant("meeting-1", "EMP001", "EMP001"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_CANNOT_REMOVE_HOST));

        verify(meetingParticipantRepository, never()).delete(any());
    }

    @Test
    void removeParticipant_대상이참석자가아니면_예외가발생한다() {
        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));
        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.removeParticipant("meeting-1", "EMP001", "EMP999"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_PARTICIPANT_NOT_FOUND));
    }

    @Test
    void declineMeeting_참석자가아니면_예외가발생한다() {
        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.declineMeeting("meeting-1", "EMP999"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_NOT_PARTICIPANT));
    }

    @Test
    void declineMeeting_주최자가거절을시도하면_예외가발생한다() {
        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP001"))
                .thenReturn(Optional.of(host));

        assertThatThrownBy(() -> meetingService.declineMeeting("meeting-1", "EMP001"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_HOST_CANNOT_DECLINE));

        assertThat(host.getStatus()).isEqualTo(ParticipantStatus.PENDING);
    }

    @Test
    void declineMeeting_성공하면_참석자상태가DECLINED로변경된다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP002"))
                .thenReturn(Optional.of(attendee));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host, attendee));

        SubmitAvailableTimesResponse response = meetingService.declineMeeting("meeting-1", "EMP002");

        assertThat(attendee.getStatus()).isEqualTo(ParticipantStatus.DECLINED);
        assertThat(response.status()).isEqualTo(ParticipantStatus.DECLINED);
        assertThat(response.allParticipantsResponded()).isFalse();
    }

    @Test
    void declineMeeting_전원이응답또는거절을완료하면_자동확정로직이실행된다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .workspace(ws)
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP002"))
                .thenReturn(Optional.of(attendee));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host, attendee));

        LocalDateTime rangeStart = LocalDateTime.of(2026, 6, 15, 9, 0);
        LocalDateTime rangeEnd = LocalDateTime.of(2026, 6, 15, 11, 0);

        when(participantAvailableTimeRepository.findByMeetingParticipant_MeetingId("meeting-1"))
                .thenReturn(List.of(
                        ParticipantAvailableTime.builder()
                                .id("time-host")
                                .meetingParticipant(host)
                                .startTime(rangeStart)
                                .endTime(rangeEnd)
                                .build()));

        SubmitAvailableTimesResponse response = meetingService.declineMeeting("meeting-1", "EMP002");

        assertThat(response.allParticipantsResponded()).isTrue();
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CONFIRMED);
        assertThat(meeting.getConfirmedStartTime()).isEqualTo(rangeStart);
        assertThat(meeting.getConfirmedEndTime()).isEqualTo(rangeStart.plusMinutes(60));
    }

    @Test
    void submitAvailableTimes_거절후재제출하면_RESPONDED로되돌아간다() {
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.GATHERING)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.DECLINED)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP002"))
                .thenReturn(Optional.of(attendee));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(attendee));

        LocalDateTime rangeStart = LocalDateTime.of(2026, 6, 15, 9, 0);
        LocalDateTime rangeEnd = LocalDateTime.of(2026, 6, 15, 10, 0);

        SubmitAvailableTimesRequest request = new SubmitAvailableTimesRequest(
                List.of(new TimeSlotRequest(rangeStart, rangeEnd)));

        meetingService.submitAvailableTimes("meeting-1", "EMP002", request);

        assertThat(attendee.getStatus()).isEqualTo(ParticipantStatus.RESPONDED);
    }

    @Test
    void findMeetingIdsDueForReminder_조건에맞는회의ID목록을반환한다() {
        Meeting meeting1 = Meeting.builder().id("meeting-1").title("A").durationMinutes(30).status(MeetingStatus.CONFIRMED).build();
        Meeting meeting2 = Meeting.builder().id("meeting-2").title("B").durationMinutes(30).status(MeetingStatus.CONFIRMED).build();

        LocalDateTime windowStart = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 6, 15, 10, 10);

        when(meetingRepository.findByStatusAndReminderSentFalseAndConfirmedStartTimeBetween(
                MeetingStatus.CONFIRMED, windowStart, windowEnd))
                .thenReturn(List.of(meeting1, meeting2));

        List<String> result = meetingService.findMeetingIdsDueForReminder(windowStart, windowEnd);

        assertThat(result).containsExactly("meeting-1", "meeting-2");
    }

    @Test
    void sendReminder_CONFIRMED이고미발송이면_참석자전원에게알림을보내고reminderSent를true로설정한다() {
        Workspace ws = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        Meeting meeting = Meeting.builder()
                .id("meeting-1")
                .workspace(ws)
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.CONFIRMED)
                .build();

        MeetingParticipant host = MeetingParticipant.builder()
                .id("participant-host")
                .meeting(meeting)
                .employeeId("EMP001")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.HOST)
                .build();

        MeetingParticipant attendee = MeetingParticipant.builder()
                .id("participant-attendee")
                .meeting(meeting)
                .employeeId("EMP002")
                .status(ParticipantStatus.RESPONDED)
                .role(ParticipantRole.ATTENDEE)
                .build();

        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.findByMeetingId("meeting-1")).thenReturn(List.of(host, attendee));

        meetingService.sendReminder("meeting-1");

        assertThat(meeting.isReminderSent()).isTrue();
        verify(notificationService, times(2)).createNotification(any(User.class), eq("MEETING_REMINDER"),
                eq("meeting-1"), eq("회의가 곧 시작합니다"), eq("참여 중인 회의가 곧 시작됩니다."), any());
    }

    @Test
    void sendReminder_이미발송되었거나CONFIRMED가아니면_아무것도하지않는다() {
        Meeting alreadySent = Meeting.builder()
                .id("meeting-1")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.CONFIRMED)
                .reminderSent(true)
                .build();

        Meeting notConfirmed = Meeting.builder()
                .id("meeting-2")
                .title("주간 회의")
                .durationMinutes(60)
                .status(MeetingStatus.SELECTING)
                .build();

        when(meetingRepository.findById("meeting-1")).thenReturn(Optional.of(alreadySent));
        when(meetingRepository.findById("meeting-2")).thenReturn(Optional.of(notConfirmed));

        meetingService.sendReminder("meeting-1");
        meetingService.sendReminder("meeting-2");

        verify(notificationService, never()).createNotification(any(), eq("MEETING_REMINDER"), any(), any(), any(), any());
        verify(meetingParticipantRepository, never()).findByMeetingId(any());
    }

    @Test
    void createMeeting_참석자에게MEETING_INVITED알림이전송된다_생성자본인제외() {
        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        when(workspaceContextValidator.getValidWorkspace("ws-1")).thenReturn(workspace);

        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> {
            Meeting m = invocation.getArgument(0);
            ReflectionTestUtils.setField(m, "id", "meeting-new");
            return m;
        });

        MeetingParticipant hostParticipant = MeetingParticipant.builder()
                .id("p-host").employeeId("EMP001").role(ParticipantRole.HOST)
                .status(ParticipantStatus.PENDING).build();
        MeetingParticipant attendee1 = MeetingParticipant.builder()
                .id("p-att1").employeeId("EMP002").role(ParticipantRole.ATTENDEE)
                .status(ParticipantStatus.PENDING).build();
        MeetingParticipant attendee2 = MeetingParticipant.builder()
                .id("p-att2").employeeId("EMP003").role(ParticipantRole.ATTENDEE)
                .status(ParticipantStatus.PENDING).build();

        when(meetingParticipantRepository.findByMeetingId("meeting-new"))
                .thenReturn(List.of(hostParticipant, attendee1, attendee2));

        CreateMeetingRequest request = new CreateMeetingRequest(
                "신규 회의", "목적", "아젠다", "회의실 A", 60,
                List.of("EMP002", "EMP003"), null);

        CreateMeetingResponse response = meetingService.createMeeting("ws-1", "EMP001", request);

        assertThat(response.meetingId()).isEqualTo("meeting-new");

        verify(notificationService, times(2)).createNotification(
                any(User.class), eq("MEETING_INVITED"), eq("meeting-new"),
                eq("회의에 초대되었습니다"), eq("새 회의에 참석자로 초대되었습니다."), eq("ws-1"));
    }

    @Test
    void createMeeting_참석자가없으면_알림이전송되지않는다() {
        Workspace workspace = Workspace.builder().id("ws-1").name("테스트 워크스페이스").build();
        when(workspaceContextValidator.getValidWorkspace("ws-1")).thenReturn(workspace);

        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> {
            Meeting m = invocation.getArgument(0);
            ReflectionTestUtils.setField(m, "id", "meeting-solo");
            return m;
        });

        MeetingParticipant hostParticipant = MeetingParticipant.builder()
                .id("p-host").employeeId("EMP001").role(ParticipantRole.HOST)
                .status(ParticipantStatus.PENDING).build();

        when(meetingParticipantRepository.findByMeetingId("meeting-solo"))
                .thenReturn(List.of(hostParticipant));

        CreateMeetingRequest request = new CreateMeetingRequest(
                "혼자 회의", null, null, null, 30,
                List.of(), null);

        meetingService.createMeeting("ws-1", "EMP001", request);

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
    }
}
