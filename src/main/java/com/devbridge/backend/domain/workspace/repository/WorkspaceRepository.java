package com.devbridge.backend.domain.workspace.repository;

import com.devbridge.backend.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
}