package com.devbridge.backend.domain.schedule.repository;

import com.devbridge.backend.domain.schedule.entity.ParticipantAvailableTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantAvailableTimeRepository extends JpaRepository<ParticipantAvailableTime, String> {
}
