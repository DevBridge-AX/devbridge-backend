package com.devbridge.backend.domain.schedule.repository;

import com.devbridge.backend.domain.schedule.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, String> {
}
