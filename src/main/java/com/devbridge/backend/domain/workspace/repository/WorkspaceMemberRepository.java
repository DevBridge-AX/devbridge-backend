package com.devbridge.backend.domain.workspace.repository;

import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, String> {

    @Query("SELECT wm FROM WorkspaceMember wm "
            + "JOIN FETCH wm.user u "
            + "WHERE wm.workspace.id = :workspaceId "
            + "AND u.name LIKE CONCAT('%', :keyword, '%')")
    List<WorkspaceMember> searchByWorkspaceIdAndUserNameContaining(
            @Param("workspaceId") String workspaceId,
            @Param("keyword") String keyword);
}
