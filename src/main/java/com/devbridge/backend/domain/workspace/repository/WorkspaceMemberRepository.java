package com.devbridge.backend.domain.workspace.repository;

import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, String> {

    long countByWorkspace_Id(String workspaceId);

    Optional<WorkspaceMember> findByUser_IdAndWorkspace_Id(String userId, String workspaceId);

    @Query("""
            select wm
            from WorkspaceMember wm
            join fetch wm.workspace
            where wm.user.id = :userId
            order by wm.joinedAt desc
            """)
    List<WorkspaceMember> findAllWithWorkspaceByUserId(@Param("userId") String userId);

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

    @Query("SELECT wm FROM WorkspaceMember wm "
            + "JOIN FETCH wm.user u "
            + "WHERE wm.workspace.id = :workspaceId "
            + "AND u.employeeId <> :employeeId "
            + "AND u.name LIKE CONCAT('%', :keyword, '%')")
    List<WorkspaceMember> searchByWorkspaceIdAndUserNameContaining(
            @Param("workspaceId") String workspaceId,
            @Param("employeeId") String employeeId,
            @Param("keyword") String keyword);

    boolean existsByWorkspace_IdAndUser_EmployeeId(String workspaceId, String employeeId);
}