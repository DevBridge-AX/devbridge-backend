package com.devbridge.backend.domain.schedule.repository;

import com.devbridge.backend.domain.schedule.entity.MeetingParticipant;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, String> {

    Optional<MeetingParticipant> findByMeetingIdAndEmployeeId(String meetingId, String employeeId);

    List<MeetingParticipant> findByMeetingId(String meetingId);

    List<MeetingParticipant> findByEmployeeIdAndMeeting_Workspace_Id(String employeeId, String workspaceId);

    List<MeetingParticipant> findByEmployeeIdAndMeeting_StatusAndMeeting_Workspace_Id(
            String employeeId, MeetingStatus status, String workspaceId);

    List<MeetingParticipant> findByEmployeeIdAndMeeting_StatusAndMeeting_Workspace_IdAndMeeting_ConfirmedStartTimeLessThanAndMeeting_ConfirmedEndTimeGreaterThan(
            String employeeId,
            MeetingStatus status,
            String workspaceId,
            LocalDateTime confirmedStartTimeBefore,
            LocalDateTime confirmedEndTimeAfter);
}
