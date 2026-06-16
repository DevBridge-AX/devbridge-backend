package com.devbridge.backend.domain.chat.entity;

import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "MESSAGE_CITATIONS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MessageCitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 인용 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message; // 대상 메시지

    @Column(name = "source_type", nullable = false, length = 20)
    private CitationSourceType sourceType; // document, git_commit, db_schema

    @Column(name = "source_id", nullable = false)
    private Integer sourceId; // 원본 테이블 PK (FK 제약 없음)

    @Column(name = "vector_chunk_id", nullable = false)
    private Integer vectorChunkId; // document_chunks.id (FK 제약 없음)

    @Column(name = "title", nullable = false, length = 255)
    private String title; // 프론트 표시용 라벨

    @Column(name = "similarity_score", precision = 5, scale = 4)
    private BigDecimal similarityScore; // 유사도
}
