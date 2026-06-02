package com.devbridge.backend.domain.chat.entity;

import com.devbridge.backend.domain.datasource.entity.DatabaseSchema;
import com.devbridge.backend.domain.datasource.entity.GitCommit;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private KnowledgeDocument document; // 참조 문서 (RAG)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "db_schema_id")
    private DatabaseSchema dbSchema; // 참조 스키마 (Text2SQL)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_commit_id")
    private GitCommit gitCommit; // 참조 커밋

    @Column(name = "vector_chunk_id", length = 255)
    private String vectorChunkId; // Vector DB 청크 ID

    @Column(name = "similarity_score", precision = 5, scale = 4)
    private BigDecimal similarityScore; // 유사도
}
