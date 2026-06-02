package com.devbridge.backend.domain.datasource.entity;

import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "GIT_COMMITS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GitCommit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 커밋 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private DataSource dataSource; // 데이터 소스 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author; // 작성자 ID

    @Column(name = "commit_hash", nullable = false, length = 100)
    private String commitHash; // 커밋 해시

    @Column(name = "commit_message", nullable = false, columnDefinition = "TEXT")
    private String commitMessage; // 메시지

    @Column(name = "pushed_at", nullable = false)
    private LocalDateTime pushedAt; // Push 시각
}
