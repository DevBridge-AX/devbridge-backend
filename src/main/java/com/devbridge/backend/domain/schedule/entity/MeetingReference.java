package com.devbridge.backend.domain.schedule.entity;

import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MEETING_REFERENCES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MeetingReference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 참조 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting; // 회의 ID

    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId; // 등록자 사번 (미가입자 대응을 위한 employeeId 표준 유지)

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 50)
    private ReferenceType referenceType; // DIRECT_FILE, DOC_LINK, EXTERNAL_LINK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private KnowledgeDocument document; // 사내 문서 연결 시 KNOWLEDGE_DOCUMENTS ID

    @Column(name = "file_url", length = 1000)
    private String fileUrl; // 직접 업로드 S3 링크 또는 외부 URL

    @Column(name = "title", nullable = false, length = 255)
    private String title; // 화면에 보여줄 제목
}
