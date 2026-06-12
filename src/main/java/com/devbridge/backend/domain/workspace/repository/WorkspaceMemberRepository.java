package com.devbridge.backend.domain.workspace.repository;

import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, String> {

    long countByWorkspace_Id(String workspaceId);

    Optional<WorkspaceMember> findByUser_IdAndWorkspace_Id(String userId, String workspaceId);

    @Query("""
            select wm
            from WorkspaceMember wm
            join fetch wm.workspace
            where wm.user.id = :userId
            order by coalesce(wm.lastAccessedAt, wm.joinedAt) desc
            """)
    List<WorkspaceMember> findRecentWorkspaceMembershipsByUserId(
            @Param("userId") String userId,
            Pageable pageable
    );
}