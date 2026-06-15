package com.devbridge.backend.domain.schedule.repository;

import com.devbridge.backend.domain.schedule.entity.MeetingReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingReferenceRepository extends JpaRepository<MeetingReference, String> {

    List<MeetingReference> findByMeetingIdOrderByCreatedAtDesc(String meetingId);

    Optional<MeetingReference> findByIdAndMeetingId(String id, String meetingId);
}
