package com.devbridge.backend.domain.chat.repository;

import com.devbridge.backend.domain.chat.entity.OwnerConfirmation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerConfirmationRepository extends JpaRepository<OwnerConfirmation, String> {

    boolean existsByQuestionMessage_IdAndStatus(String questionMessageId, String status);

    Optional<OwnerConfirmation> findByIdAndDeletedAtIsNull(String id);

    Page<OwnerConfirmation> findByAssignedOwner_EmployeeIdAndWorkspace_IdAndDeletedAtIsNull(
            String employeeId, String workspaceId, Pageable pageable);

    Page<OwnerConfirmation> findByRequester_EmployeeIdAndWorkspace_IdAndDeletedAtIsNull(
            String employeeId, String workspaceId, Pageable pageable);
}
