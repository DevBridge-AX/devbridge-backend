package com.devbridge.backend.domain.schedule.service;

import com.devbridge.backend.domain.notification.service.NotificationService;
import com.devbridge.backend.domain.schedule.dto.CandidateTimeSlot;
import com.devbridge.backend.domain.schedule.dto.ConfirmedScheduleResponse;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingRequest;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingResponse;
import com.devbridge.backend.domain.schedule.dto.MeetingDetailResponse;
import com.devbridge.backend.domain.schedule.dto.MeetingParticipantResponse;
import com.devbridge.backend.domain.schedule.dto.MeetingSummaryResponse;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesRequest;
import com.devbridge.backend.domain.schedule.dto.SubmitAvailableTimesResponse;
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
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.service.UserInternalService;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.service.WorkspaceContextValidator;
import com.devbridge.backend.domain.workspace.service.WorkspaceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final ParticipantAvailableTimeRepository participantAvailableTimeRepository;
    private final WorkspaceContextValidator workspaceContextValidator;
    private final WorkspaceService workspaceService;
    private final MeetingReferenceService meetingReferenceService;
    private final ObjectMapper objectMapper;
    private final UserInternalService userInternalService;
    private final NotificationService notificationService;

    private static final String NOTIFICATION_TYPE_MEETING_INVITED = "MEETING_INVITED";
    private static final String NOTIFICATION_TYPE_MEETING_UPDATED = "MEETING_UPDATED";

    private User resolveUser(String employeeId) {
        return userInternalService.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다: " + employeeId));
    }

    @Transactional
    public CreateMeetingResponse createMeeting(
            String workspaceId,
            String hostEmployeeId,
            CreateMeetingRequest request
    ) {
        User host = resolveUser(hostEmployeeId);

        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);
        String validWorkspaceId = workspace.getId();

        workspaceService.validateMembership(validWorkspaceId, hostEmployeeId);

        Meeting meeting = Meeting.builder()
                .workspace(workspace)
                .title(request.title())
                .purpose(request.purpose())
                .agenda(request.agenda())
                .location(request.location())
                .durationMinutes(request.durationMinutes())
                .status(MeetingStatus.GATHERING)
                .build();

        meetingRepository.save(meeting);

        List<MeetingParticipant> participants = new ArrayList<>();
        participants.add(MeetingParticipant.builder()
                .meeting(meeting)
                .employeeId(host.getEmployeeId())
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

        notifyParticipants(meeting.getId(), hostEmployeeId,
                NOTIFICATION_TYPE_MEETING_INVITED,
                "회의에 초대되었습니다",
                "새 회의에 참석자로 초대되었습니다.",
                workspaceId);

        if (request.references() != null) {
            request.references().forEach(referenceRequest ->
                    meetingReferenceService.createReference(meeting, host.getEmployeeId(), referenceRequest));
        }

        return new CreateMeetingResponse(meeting.getId());
    }

    @Transactional(readOnly = true)
    public List<ConfirmedScheduleResponse> getMyConfirmedSchedules(
            String workspaceId,
            String employeeId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        resolveUser(employeeId);

        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);
        String validWorkspaceId = workspace.getId();

        workspaceService.validateMembership(validWorkspaceId, employeeId);

        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.plusDays(1).atStartOfDay();

        return meetingParticipantRepository
                .findByEmployeeIdAndMeeting_StatusAndMeeting_Workspace_IdAndMeeting_ConfirmedStartTimeLessThanAndMeeting_ConfirmedEndTimeGreaterThan(
                        employeeId,
                        MeetingStatus.CONFIRMED,
                        validWorkspaceId,
                        rangeEnd,
                        rangeStart
                )
                .stream()
                .map(participant -> {
                    Meeting meeting = participant.getMeeting();
                    return new ConfirmedScheduleResponse(
                            meeting.getId(),
                            meeting.getTitle(),
                            meeting.getConfirmedStartTime(),
                            meeting.getConfirmedEndTime());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeetingSummaryResponse> getMyMeetings(
            String workspaceId,
            String employeeId,
            MeetingStatus status
    ) {
        resolveUser(employeeId);

        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);
        String validWorkspaceId = workspace.getId();

        workspaceService.validateMembership(validWorkspaceId, employeeId);

        List<MeetingParticipant> participants = status == null
                ? meetingParticipantRepository.findByEmployeeIdAndMeeting_Workspace_Id(employeeId, validWorkspaceId)
                : meetingParticipantRepository.findByEmployeeIdAndMeeting_StatusAndMeeting_Workspace_Id(
                        employeeId,
                        status,
                        validWorkspaceId
                );

        return participants.stream()
                .map(MeetingParticipant::getMeeting)
                .sorted(Comparator.comparing(Meeting::getCreatedAt).reversed())
                .map(meeting -> new MeetingSummaryResponse(
                        meeting.getId(),
                        meeting.getTitle(),
                        meeting.getStatus(),
                        meeting.getDurationMinutes(),
                        meeting.getConfirmedStartTime(),
                        meeting.getConfirmedEndTime()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MeetingDetailResponse getMeetingDetail(String meetingId, String employeeId) {
        resolveUser(employeeId);

        meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회의의 참석자가 아닙니다."));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회의가 존재하지 않습니다."));

        return buildDetailResponse(meeting);
    }

    @Transactional
    public MeetingDetailResponse updateMeeting(String meetingId, String employeeId, UpdateMeetingRequest request) {
        resolveUser(employeeId);

        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회의의 참석자가 아닙니다."));

        if (participant.getRole() != ParticipantRole.HOST) {
            throw new IllegalArgumentException("회의 주최자만 회의 정보를 수정할 수 있습니다.");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회의가 존재하지 않습니다."));

        meeting.updateInfo(request.title(), request.purpose(), request.agenda(), request.location());

        notifyParticipants(meetingId, employeeId,
                NOTIFICATION_TYPE_MEETING_UPDATED,
                "회의 일정이 변경되었습니다",
                "참여 중인 회의의 상세 정보가 변경되었습니다.",
                meeting.getWorkspace().getId());

        return buildDetailResponse(meeting);
    }

    private void notifyParticipants(String meetingId, String actorEmployeeId,
                                    String notificationType, String title, String message,
                                    String workspaceId) {
        meetingParticipantRepository.findByMeetingId(meetingId).stream()
                .filter(p -> !p.getEmployeeId().equals(actorEmployeeId))
                .forEach(p -> userInternalService.findByEmployeeId(p.getEmployeeId())
                        .ifPresent(recipient -> notificationService.createNotification(
                                recipient, notificationType, meetingId, title, message, workspaceId)));
    }

    private MeetingDetailResponse buildDetailResponse(Meeting meeting) {
        List<MeetingParticipantResponse> participants = meetingParticipantRepository.findByMeetingId(meeting.getId()).stream()
                .map(participant -> {
                    var user = userInternalService.findByEmployeeId(participant.getEmployeeId()).orElse(null);
                    return new MeetingParticipantResponse(
                            participant.getEmployeeId(),
                            user != null ? user.getName() : null,
                            user != null ? user.getDepartment() : null,
                            user != null ? user.getPosition() : null,
                            participant.getRole(),
                            participant.getStatus());
                })
                .toList();

        return new MeetingDetailResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getPurpose(),
                meeting.getAgenda(),
                meeting.getLocation(),
                meeting.getDurationMinutes(),
                meeting.getStatus(),
                meeting.getConfirmedStartTime(),
                meeting.getConfirmedEndTime(),
                fromJson(meeting.getTopCandidateTimes()),
                participants,
                meetingReferenceService.getReferences(meeting.getId()));
    }

    @Transactional
    public SubmitAvailableTimesResponse submitAvailableTimes(
            String meetingId,
            String employeeId,
            SubmitAvailableTimesRequest request
    ) {
        resolveUser(employeeId);

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

        if (allResponded) {
            selectTopCandidateTimes(participant.getMeeting());
        }

        return new SubmitAvailableTimesResponse(meetingId, employeeId, participant.getStatus(), allResponded);
    }

    private boolean isAllParticipantsResponded(String meetingId) {
        return meetingParticipantRepository.findByMeetingId(meetingId).stream()
                .allMatch(participant -> participant.getStatus() == ParticipantStatus.RESPONDED);
    }

    private void selectTopCandidateTimes(Meeting meeting) {
        List<ParticipantAvailableTime> availableTimes =
                participantAvailableTimeRepository.findByMeetingParticipant_MeetingId(meeting.getId());

        List<List<TimeRange>> rangesByParticipant = availableTimes.stream()
                .collect(Collectors.groupingBy(time -> time.getMeetingParticipant().getId()))
                .values().stream()
                .map(times -> mergeRanges(times.stream()
                        .map(time -> new TimeRange(time.getStartTime(), time.getEndTime()))
                        .toList()))
                .toList();

        List<TimeRange> intersection = rangesByParticipant.stream()
                .reduce(this::intersectRanges)
                .orElse(List.of());

        List<TimeRange> sufficientRanges = intersection.stream()
                .filter(range -> Duration.between(range.start(), range.end()).toMinutes() >= meeting.getDurationMinutes())
                .sorted(Comparator.comparing(TimeRange::start))
                .toList();

        List<CandidateTimeSlot> topCandidates = sufficientRanges.stream()
                .limit(3)
                .map(range -> new CandidateTimeSlot(range.start(), range.end()))
                .toList();

        meeting.selectTopCandidateTimes(toJson(topCandidates));

        if (sufficientRanges.isEmpty()) {
            log.warn("회의 ID {}의 참석자 가능 시간 교집합 중 소요 시간을 만족하는 후보가 없어 SELECTING 상태로 유지됩니다.", meeting.getId());
            return;
        }

        TimeRange bestRange = sufficientRanges.get(0);
        meeting.confirmSchedule(bestRange.start(), bestRange.start().plusMinutes(meeting.getDurationMinutes()));
    }

    private List<TimeRange> mergeRanges(List<TimeRange> ranges) {
        List<TimeRange> sorted = ranges.stream()
                .sorted(Comparator.comparing(TimeRange::start))
                .toList();

        List<TimeRange> merged = new ArrayList<>();
        for (TimeRange range : sorted) {
            if (!merged.isEmpty() && !range.start().isAfter(merged.get(merged.size() - 1).end())) {
                TimeRange last = merged.remove(merged.size() - 1);
                LocalDateTime mergedEnd = last.end().isAfter(range.end()) ? last.end() : range.end();
                merged.add(new TimeRange(last.start(), mergedEnd));
            } else {
                merged.add(range);
            }
        }

        return merged;
    }

    private List<TimeRange> intersectRanges(List<TimeRange> ranges1, List<TimeRange> ranges2) {
        List<TimeRange> result = new ArrayList<>();

        for (TimeRange range1 : ranges1) {
            for (TimeRange range2 : ranges2) {
                LocalDateTime start = range1.start().isAfter(range2.start()) ? range1.start() : range2.start();
                LocalDateTime end = range1.end().isBefore(range2.end()) ? range1.end() : range2.end();

                if (start.isBefore(end)) {
                    result.add(new TimeRange(start, end));
                }
            }
        }

        return result;
    }

    private String toJson(List<CandidateTimeSlot> candidateTimeSlots) {
        try {
            return objectMapper.writeValueAsString(candidateTimeSlots);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("후보 시간 목록 직렬화에 실패했습니다.", e);
        }
    }

    private List<CandidateTimeSlot> fromJson(String topCandidateTimesJson) {
        if (topCandidateTimesJson == null || topCandidateTimesJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(topCandidateTimesJson, new TypeReference<List<CandidateTimeSlot>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("후보 시간 목록 역직렬화에 실패했습니다.", e);
        }
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {
    }
}