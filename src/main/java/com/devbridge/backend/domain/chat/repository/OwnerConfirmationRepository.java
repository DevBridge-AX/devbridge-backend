package com.devbridge.backend.domain.chat.repository;

import com.devbridge.backend.domain.chat.entity.OwnerConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OwnerConfirmationRepository extends JpaRepository<OwnerConfirmation, String> {
}
