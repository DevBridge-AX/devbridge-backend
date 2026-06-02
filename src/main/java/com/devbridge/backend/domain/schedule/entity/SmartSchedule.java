package com.devbridge.backend.domain.schedule.entity;

import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "SMART_SCHEDULES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE SMART_SCHEDULES SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class SmartSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 일정 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace; // 워크스페이스 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester; // 회의 요청자 ID

    @Column(name = "title", nullable = false, length = 255)
    private String title; // 회의 제목

    @Column(name = "top_candidate_times", columnDefinition = "json")
    private String topCandidateTimes; // 시스템 산출 Top 3 시간

    @Column(name = "confirmed_time")
    private LocalDateTime confirmedTime; // 최종 확정 시간

    @Column(name = "meeting_link", length = 1000)
    private String meetingLink; // 화상회의 링크

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary; // AI 회의/문서 요약본 캐싱

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "GATHERING"; // GATHERING, SELECTING, CONFIRMED, COMPLETED, CANCELLED
}
