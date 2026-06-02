package com.devbridge.backend.domain.datasource.entity;

import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DATABASE_SCHEMAS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DatabaseSchema extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 스키마 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private DataSource dataSource; // 데이터 소스 ID

    @Column(name = "db_name", nullable = false, length = 100)
    private String dbName; // 대상 DB명

    @Column(name = "schema_ddl", nullable = false, columnDefinition = "TEXT")
    private String schemaDdl; // DDL 메타데이터
}
