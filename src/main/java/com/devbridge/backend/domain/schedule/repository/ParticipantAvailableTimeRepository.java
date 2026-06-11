package com.devbridge.backend.domain.schedule.repository;

import com.devbridge.backend.domain.schedule.entity.ParticipantAvailableTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantAvailableTimeRepository extends JpaRepository<ParticipantAvailableTime, String> {

    List<ParticipantAvailableTime> findByMeetingParticipant_MeetingId(String meetingId);
}
