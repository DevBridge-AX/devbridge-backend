package com.devbridge.backend.domain.datasource.entity;

import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "DATA_SOURCES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE DATA_SOURCES SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class DataSource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 소스 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace; // 워크스페이스 ID

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType; // DOC, GIT, DATABASE

    @Column(name = "source_name", nullable = false, length = 255)
    private String sourceName; // 소스명

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "CONNECTED"; // 연동 상태

    // secret, token, password 등 민감 정보는 저장하지 않는다
    @Column(name = "config", columnDefinition = "JSON")
    private String config; // 연동 설정 (비민감 정보만)
}
