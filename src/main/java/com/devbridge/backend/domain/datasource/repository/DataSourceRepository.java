package com.devbridge.backend.domain.datasource.repository;

import com.devbridge.backend.domain.datasource.entity.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSourceRepository extends JpaRepository<DataSource, String> {

    List<DataSource> findByWorkspace_Id(String workspaceId);

    List<DataSource> findByWorkspace_IdAndSourceType(String workspaceId, String sourceType);
}