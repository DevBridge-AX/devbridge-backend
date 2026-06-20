package com.devbridge.backend.domain.workspace.repository;

import com.devbridge.backend.domain.workspace.entity.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, String> {

    boolean existsByWorkspace_IdAndInvitedEmailAndStatus(
            String workspaceId,
            String invitedEmail,
            String status
    );

    @Query("""
            select wi
            from WorkspaceInvitation wi
            join fetch wi.workspace
            where lower(wi.invitedEmail) = lower(:email)
              and wi.status = :status
            order by wi.createdAt desc
            """)
    List<WorkspaceInvitation> findReceivedInvitations(
            @Param("email") String email,
            @Param("status") String status
    );

    @Query("""
            select wi
            from WorkspaceInvitation wi
            join fetch wi.workspace
            where wi.id = :invitationId
            """)
    Optional<WorkspaceInvitation> findWithWorkspaceById(
            @Param("invitationId") String invitationId
    );
}