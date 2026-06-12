package com.devbridge.backend.domain.schedule.service;

import com.devbridge.backend.domain.schedule.dto.CandidateTimeSlot;
import com.devbridge.backend.domain.schedule.dto.MeetingDetailResponse;
import com.devbridge.backend.domain.schedule.dto.MeetingSummaryResponse;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesRequest;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesResponse;
import com.devbridge.backend.domain.schedule.dto.TimeSlotRequest;
import com.devbridge.backend.domain.schedule.entity.Meeting;
import com.devbridge.backend.domain.schedule.entity.MeetingParticipant;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import com.devbridge.backend.domain.schedule.entity.ParticipantAvailableTime;
import com.devbridge.backend.domain.schedule.entity.ParticipantRole;
import com.devbridge.backend.domain.schedule.entity.ParticipantStatus;
import com.devbridge.backend.domain.schedule.repository.MeetingParticipantRepository;
import com.devbridge.backend.domain.schedule.repository.MeetingRepository;
import com.devbridge.backend.domain.schedule.repository.ParticipantAvailableTimeRepository;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import com.devbridge.backend.domain.workspace.service.WorkspaceService;
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
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceService workspaceService;

    private ObjectMapper objectMapper;
    private MeetingService meetingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        meetingService = new MeetingService(
                meetingRepository, meetingParticipantRepository, participantAvailableTimeRepository, workspaceRepository, workspaceService, objectMapper);
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

        when(meetingParticipantRepository.findByEmployeeId("EMP001"))
                .thenReturn(List.of(participantOnOlder, participantOnNewer));

        List<MeetingSummaryResponse> result = meetingService.getMyMeetings("EMP001", null);

        assertThat(result).extracting(MeetingSummaryResponse::meetingId)
                .containsExactly("meeting-new", "meeting-old");
        assertThat(result.get(1).status()).isEqualTo(MeetingStatus.CONFIRMED);
    }

    @Test
    void getMyMeetings_status가주어지면_해당상태의회의만조회한다() {
        when(meetingParticipantRepository.findByEmployeeIdAndMeeting_Status("EMP001", MeetingStatus.CONFIRMED))
                .thenReturn(List.of());

        List<MeetingSummaryResponse> result = meetingService.getMyMeetings("EMP001", MeetingStatus.CONFIRMED);

        assertThat(result).isEmpty();
        verify(meetingParticipantRepository).findByEmployeeIdAndMeeting_Status("EMP001", MeetingStatus.CONFIRMED);
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

        MeetingDetailResponse response = meetingService.getMeetingDetail("meeting-1", "EMP002");

        assertThat(response.meetingId()).isEqualTo("meeting-1");
        assertThat(response.status()).isEqualTo(MeetingStatus.CONFIRMED);
        assertThat(response.confirmedStartTime()).isEqualTo(confirmedStart);
        assertThat(response.confirmedEndTime()).isEqualTo(confirmedEnd);
        assertThat(response.topCandidateTimes()).containsExactly(new CandidateTimeSlot(confirmedStart, confirmedEnd));
        assertThat(response.participants()).extracting("employeeId")
                .containsExactlyInAnyOrder("EMP001", "EMP002");
    }

    @Test
    void getMeetingDetail_참석자가아니면_예외가발생한다() {
        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId("meeting-1", "EMP999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.getMeetingDetail("meeting-1", "EMP999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 회의의 참석자가 아닙니다.");
    }
}
