package com.devbridge.backend.domain.schedule.repository;

import com.devbridge.backend.domain.schedule.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, String> {
}
