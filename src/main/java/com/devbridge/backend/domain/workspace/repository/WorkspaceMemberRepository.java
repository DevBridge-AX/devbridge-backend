package com.devbridge.backend.domain.workspace.repository;

import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, String> {

    long countByWorkspace_Id(String workspaceId);
}