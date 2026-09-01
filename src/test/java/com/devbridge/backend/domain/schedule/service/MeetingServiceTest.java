package com.devbridge.backend.domain.schedule.service;

import com.devbridge.backend.domain.schedule.dto.CandidateTimeSlot;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingRequest;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingResponse;
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
        Meeting meeting = Meeting.builder()
                .id("meeting-2")
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

        UpdateMeetingRequest request = new UpdateMeetingRequest("수정된 회의", "목적", "아젠다", "회의실 A");

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

        UpdateMeetingRequest request = new UpdateMeetingRequest("수정된 회의", "목적", "아젠다", "회의실 A");

        assertThatThrownBy(() -> meetingService.updateMeeting("meeting-1", "EMP002", request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("회의 주최자만 회의 정보를 수정할 수 있습니다.")
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SCHEDULE_NOT_HOST));

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any());
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
