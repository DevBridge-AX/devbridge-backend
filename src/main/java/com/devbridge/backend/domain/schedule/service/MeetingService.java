package com.devbridge.backend.domain.schedule.service;

import com.devbridge.backend.domain.schedule.dto.CreateMeetingRequest;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingResponse;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesRequest;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesResponse;
import com.devbridge.backend.domain.schedule.entity.Meeting;
import com.devbridge.backend.domain.schedule.entity.MeetingParticipant;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import com.devbridge.backend.domain.schedule.entity.ParticipantAvailableTime;
import com.devbridge.backend.domain.schedule.entity.ParticipantRole;
import com.devbridge.backend.domain.schedule.entity.ParticipantStatus;
import com.devbridge.backend.domain.schedule.repository.MeetingParticipantRepository;
import com.devbridge.backend.domain.schedule.repository.MeetingRepository;
import com.devbridge.backend.domain.schedule.repository.ParticipantAvailableTimeRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final ParticipantAvailableTimeRepository participantAvailableTimeRepository;
    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public CreateMeetingResponse createMeeting(CreateMeetingRequest request) {
//        Workspace workspace = workspaceRepository.findById(request.workspaceId())
//                .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스가 존재하지 않습니다."));

        Meeting meeting = Meeting.builder()
//                .workspace(workspace)
                .title(request.title())
                .durationMinutes(request.durationMinutes())
                .status(MeetingStatus.GATHERING)
                .build();
        meetingRepository.save(meeting);

        List<MeetingParticipant> participants = new ArrayList<>();
        participants.add(MeetingParticipant.builder()
                .meeting(meeting)
                .employeeId(request.hostEmployeeId())
                .status(ParticipantStatus.PENDING)
                .role(ParticipantRole.HOST)
                .build());

        request.participantEmployeeIds().forEach(employeeId ->
                participants.add(MeetingParticipant.builder()
                        .meeting(meeting)
                        .employeeId(employeeId)
                        .status(ParticipantStatus.PENDING)
                        .role(ParticipantRole.ATTENDEE)
                        .build())
        );

        meetingParticipantRepository.saveAll(participants);

        return new CreateMeetingResponse(meeting.getId());
    }

    @Transactional
    public SubmitAvailableTimesResponse submitAvailableTimes(
            String meetingId, String employeeId, SubmitAvailableTimesRequest request) {

        MeetingParticipant participant = meetingParticipantRepository
                .findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회의의 참석자가 아닙니다."));

        List<ParticipantAvailableTime> availableTimes = request.availableTimes().stream()
                .map(slot -> ParticipantAvailableTime.builder()
                        .meetingParticipant(participant)
                        .startTime(slot.startTime())
                        .endTime(slot.endTime())
                        .build())
                .toList();
        participantAvailableTimeRepository.saveAll(availableTimes);

        participant.respond();

        boolean allResponded = isAllParticipantsResponded(meetingId);

        return new SubmitAvailableTimesResponse(meetingId, employeeId, participant.getStatus(), allResponded);
    }

    private boolean isAllParticipantsResponded(String meetingId) {
        return meetingParticipantRepository.findByMeetingId(meetingId).stream()
                .allMatch(participant -> participant.getStatus() == ParticipantStatus.RESPONDED);
    }
}
