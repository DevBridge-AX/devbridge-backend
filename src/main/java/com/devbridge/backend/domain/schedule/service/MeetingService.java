package com.devbridge.backend.domain.schedule.service;

import com.devbridge.backend.domain.notification.service.NotificationService;
import com.devbridge.backend.domain.schedule.dto.AddParticipantsRequest;
import com.devbridge.backend.domain.schedule.dto.CandidateTimeSlot;
import com.devbridge.backend.domain.schedule.dto.ConfirmedScheduleResponse;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingRequest;
import com.devbridge.backend.domain.schedule.dto.CreateMeetingResponse;
import com.devbridge.backend.domain.schedule.dto.ManualConfirmRequest;
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
import com.devbridge.backend.global.common.exception.BusinessException;
import com.devbridge.backend.global.common.exception.ErrorCode;
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
    private static final String NOTIFICATION_TYPE_MEETING_CANCELED = "MEETING_CANCELED";
    private static final String NOTIFICATION_TYPE_MEETING_CONFIRMED = "MEETING_CONFIRMED";
    private static final String NOTIFICATION_TYPE_MEETING_REOPENED = "MEETING_REOPENED";
    private static final String NOTIFICATION_TYPE_MEETING_REMINDER = "MEETING_REMINDER";

    private User resolveUser(String employeeId) {
        return userInternalService.findByEmployeeId(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_USER_NOT_FOUND,
                        "해당 사용자가 존재하지 않습니다: " + employeeId));
    }

    private MeetingParticipant resolveHost(String meetingId, String employeeId) {
        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_PARTICIPANT));

        if (participant.getRole() != ParticipantRole.HOST) {
            throw new BusinessException(ErrorCode.SCHEDULE_NOT_HOST);
        }

        return participant;
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
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_PARTICIPANT));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_MEETING_NOT_FOUND));

        return buildDetailResponse(meeting);
    }

    @Transactional
    public MeetingDetailResponse updateMeeting(String meetingId, String employeeId, UpdateMeetingRequest request) {
        resolveUser(employeeId);
        resolveHost(meetingId, employeeId);

        if (request.title() != null && request.title().isBlank()) {
            throw new BusinessException(ErrorCode.SCHEDULE_INVALID_TITLE);
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_MEETING_NOT_FOUND));

        // 제공된 필드만 반영한다(null은 미변경). PATCH 의미상 필드를 하나씩 개별 수정할 수 있어야 한다.
        meeting.updateInfo(request.title(), request.purpose(), request.agenda(), request.location(), request.meetingLink());

        notifyParticipants(meetingId, employeeId,
                NOTIFICATION_TYPE_MEETING_UPDATED,
                "회의 일정이 변경되었습니다",
                "참여 중인 회의의 상세 정보가 변경되었습니다.",
                meeting.getWorkspace().getId());

        return buildDetailResponse(meeting);
    }

    @Transactional
    public MeetingDetailResponse cancelMeeting(String meetingId, String employeeId) {
        resolveUser(employeeId);
        resolveHost(meetingId, employeeId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_MEETING_NOT_FOUND));

        if (meeting.getStatus() == MeetingStatus.CANCELED) {
            throw new BusinessException(ErrorCode.SCHEDULE_ALREADY_CANCELED);
        }

        meeting.cancel();

        notifyParticipants(meetingId, employeeId,
                NOTIFICATION_TYPE_MEETING_CANCELED,
                "회의가 취소되었습니다",
                "참여 중인 회의가 주최자에 의해 취소되었습니다.",
                meeting.getWorkspace().getId());

        return buildDetailResponse(meeting);
    }

    @Transactional
    public MeetingDetailResponse confirmMeetingManually(String meetingId, String employeeId, ManualConfirmRequest request) {
        resolveUser(employeeId);
        resolveHost(meetingId, employeeId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_MEETING_NOT_FOUND));

        if (meeting.getStatus() == MeetingStatus.CANCELED) {
            throw new BusinessException(ErrorCode.SCHEDULE_ALREADY_CANCELED);
        }
        if (meeting.getStatus() == MeetingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.SCHEDULE_ALREADY_CONFIRMED);
        }
        if (!request.confirmedStartTime().isBefore(request.confirmedEndTime())) {
            throw new BusinessException(ErrorCode.SCHEDULE_INVALID_TIME_RANGE);
        }

        meeting.confirmSchedule(request.confirmedStartTime(), request.confirmedEndTime());

        notifyParticipants(meetingId, employeeId,
                NOTIFICATION_TYPE_MEETING_CONFIRMED,
                "회의 일정이 확정되었습니다",
                "주최자가 회의 시간을 직접 확정했습니다.",
                meeting.getWorkspace().getId());

        return buildDetailResponse(meeting);
    }

    @Transactional
    public MeetingDetailResponse reopenMeeting(String meetingId, String employeeId) {
        resolveUser(employeeId);
        resolveHost(meetingId, employeeId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_MEETING_NOT_FOUND));

        // 자동 확정이 교집합 부재로 실패해 후보가 없는 채로 멈춘 SELECTING 상태만 재조율 대상으로 삼는다.
        if (meeting.getStatus() != MeetingStatus.SELECTING) {
            throw new BusinessException(ErrorCode.SCHEDULE_INVALID_STATUS_FOR_REOPEN);
        }

        List<ParticipantAvailableTime> existingTimes =
                participantAvailableTimeRepository.findByMeetingParticipant_MeetingId(meetingId);
        participantAvailableTimeRepository.deleteAll(existingTimes);

        meetingParticipantRepository.findByMeetingId(meetingId)
                .forEach(MeetingParticipant::resetToPending);

        meeting.reopen();

        notifyParticipants(meetingId, employeeId,
                NOTIFICATION_TYPE_MEETING_REOPENED,
                "회의 일정 재조율이 요청되었습니다",
                "가능한 시간을 다시 제출해주세요.",
                meeting.getWorkspace().getId());

        return buildDetailResponse(meeting);
    }

    @Transactional
    public MeetingDetailResponse addParticipants(String meetingId, String employeeId, AddParticipantsRequest request) {
        resolveUser(employeeId);
        resolveHost(meetingId, employeeId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_MEETING_NOT_FOUND));

        if (meeting.getStatus() == MeetingStatus.CANCELED) {
            throw new BusinessException(ErrorCode.SCHEDULE_ALREADY_CANCELED);
        }

        for (String newEmployeeId : request.employeeIds()) {
            if (meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, newEmployeeId).isPresent()) {
                throw new BusinessException(ErrorCode.SCHEDULE_PARTICIPANT_ALREADY_EXISTS,
                        "이미 참석자로 등록된 사용자입니다: " + newEmployeeId);
            }
        }

        List<MeetingParticipant> newParticipants = request.employeeIds().stream()
                .map(newEmployeeId -> MeetingParticipant.builder()
                        .meeting(meeting)
                        .employeeId(newEmployeeId)
                        .status(ParticipantStatus.PENDING)
                        .role(ParticipantRole.ATTENDEE)
                        .build())
                .toList();

        meetingParticipantRepository.saveAll(newParticipants);

        newParticipants.forEach(p -> userInternalService.findByEmployeeId(p.getEmployeeId())
                .ifPresent(recipient -> notificationService.createNotification(
                        recipient, NOTIFICATION_TYPE_MEETING_INVITED, meetingId,
                        "회의에 초대되었습니다", "새 회의에 참석자로 초대되었습니다.",
                        meeting.getWorkspace().getId())));

        return buildDetailResponse(meeting);
    }

    @Transactional
    public void removeParticipant(String meetingId, String employeeId, String targetEmployeeId) {
        resolveUser(employeeId);
        resolveHost(meetingId, employeeId);

        MeetingParticipant target = meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, targetEmployeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_PARTICIPANT_NOT_FOUND));

        if (target.getRole() == ParticipantRole.HOST) {
            throw new BusinessException(ErrorCode.SCHEDULE_CANNOT_REMOVE_HOST);
        }

        meetingParticipantRepository.delete(target);
    }

    @Transactional(readOnly = true)
    public List<String> findMeetingIdsDueForReminder(LocalDateTime windowStart, LocalDateTime windowEnd) {
        return meetingRepository
                .findByStatusAndReminderSentFalseAndConfirmedStartTimeBetween(MeetingStatus.CONFIRMED, windowStart, windowEnd)
                .stream()
                .map(Meeting::getId)
                .toList();
    }

    @Transactional
    public void sendReminder(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_MEETING_NOT_FOUND));

        // 폴링 시점과 처리 시점 사이 상태가 바뀌었을 수 있어 방어적으로 재확인한다.
        if (meeting.getStatus() != MeetingStatus.CONFIRMED || meeting.isReminderSent()) {
            return;
        }

        // 거절한 참석자는 리마인더 대상에서 제외한다(공용 notifyParticipants는 이 필터가 없다).
        meetingParticipantRepository.findByMeetingId(meetingId).stream()
                .filter(p -> p.getStatus() != ParticipantStatus.DECLINED)
                .forEach(p -> userInternalService.findByEmployeeId(p.getEmployeeId())
                        .ifPresent(recipient -> notificationService.createNotification(
                                recipient, NOTIFICATION_TYPE_MEETING_REMINDER, meetingId,
                                "회의가 곧 시작합니다", "참여 중인 회의가 곧 시작됩니다.",
                                meeting.getWorkspace().getId())));

        meeting.markReminderSent();
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
                meeting.getMeetingLink(),
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
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_PARTICIPANT));

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

    @Transactional
    public SubmitAvailableTimesResponse declineMeeting(String meetingId, String employeeId) {
        resolveUser(employeeId);

        MeetingParticipant participant = meetingParticipantRepository
                .findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_PARTICIPANT));

        if (participant.getRole() == ParticipantRole.HOST) {
            throw new BusinessException(ErrorCode.SCHEDULE_HOST_CANNOT_DECLINE);
        }

        participant.decline();

        boolean allResponded = isAllParticipantsResponded(meetingId);

        if (allResponded) {
            selectTopCandidateTimes(participant.getMeeting());
        }

        return new SubmitAvailableTimesResponse(meetingId, employeeId, participant.getStatus(), allResponded);
    }

    private boolean isAllParticipantsResponded(String meetingId) {
        return meetingParticipantRepository.findByMeetingId(meetingId).stream()
                .allMatch(participant -> participant.getStatus() != ParticipantStatus.PENDING);
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

        notifyParticipants(meeting.getId(), null,
                NOTIFICATION_TYPE_MEETING_CONFIRMED,
                "회의 일정이 확정되었습니다",
                "제출된 가능 시간을 기반으로 회의 일정이 자동으로 확정되었습니다.",
                meeting.getWorkspace().getId());
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
            throw new BusinessException(ErrorCode.SCHEDULE_CANDIDATE_SERIALIZE_FAILED,
                    "후보 시간 목록 직렬화에 실패했습니다.", e);
        }
    }

    private List<CandidateTimeSlot> fromJson(String topCandidateTimesJson) {
        if (topCandidateTimesJson == null || topCandidateTimesJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(topCandidateTimesJson, new TypeReference<List<CandidateTimeSlot>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SCHEDULE_CANDIDATE_DESERIALIZE_FAILED,
                    "후보 시간 목록 역직렬화에 실패했습니다.", e);
        }
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {
    }
}