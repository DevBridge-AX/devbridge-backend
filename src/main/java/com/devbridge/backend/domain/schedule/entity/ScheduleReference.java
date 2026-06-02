package com.devbridge.backend.domain.schedule.entity;

import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SCHEDULE_REFERENCES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ScheduleReference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 참조 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private SmartSchedule schedule; // 일정 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 등록자 ID

    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType; // DIRECT_FILE, DOC_LINK, EXTERNAL_LINK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private KnowledgeDocument document; // 사내 문서 연결 시 KNOWLEDGE_DOCUMENTS ID

    @Column(name = "file_url", length = 1000)
    private String fileUrl; // 직접 업로드 S3 링크 또는 외부 URL

    @Column(name = "title", nullable = false, length = 255)
    private String title; // 화면에 보여줄 제목
}
