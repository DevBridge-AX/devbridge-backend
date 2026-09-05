package com.devbridge.backend.domain.schedule.repository;

import com.devbridge.backend.domain.schedule.entity.Meeting;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, String> {
    boolean existsByWorkspace_IdAndTitle(String workspaceId, String title);

    List<Meeting> findByStatusAndReminderSentFalseAndConfirmedStartTimeBetween(
            MeetingStatus status, LocalDateTime start, LocalDateTime end);
}
