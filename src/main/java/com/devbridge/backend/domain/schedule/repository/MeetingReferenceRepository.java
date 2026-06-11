package com.devbridge.backend.domain.schedule.repository;

import com.devbridge.backend.domain.schedule.entity.MeetingReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingReferenceRepository extends JpaRepository<MeetingReference, String> {
}
