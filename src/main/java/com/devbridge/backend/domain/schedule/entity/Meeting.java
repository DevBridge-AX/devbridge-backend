package com.devbridge.backend.domain.schedule.entity;

import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "MEETINGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE MEETINGS SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "agenda", columnDefinition = "TEXT")
    private String agenda;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private MeetingStatus status;

    @Column(name = "confirmed_start_time")
    private LocalDateTime confirmedStartTime;

    @Column(name = "confirmed_end_time")
    private LocalDateTime confirmedEndTime;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "meeting_link", length = 1000)
    private String meetingLink;

    @Column(name = "top_candidate_times", columnDefinition = "TEXT")
    private String topCandidateTimes;

    @Builder.Default
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeetingReference> references = new ArrayList<>();

    public void updateInfo(String title, String purpose, String agenda, String location) {
        if (title != null) {
            this.title = title;
        }
        if (purpose != null) {
            this.purpose = purpose;
        }
        if (agenda != null) {
            this.agenda = agenda;
        }
        if (location != null) {
            this.location = location;
        }
    }

    public void selectTopCandidateTimes(String topCandidateTimesJson) {
        this.topCandidateTimes = topCandidateTimesJson;
        this.status = MeetingStatus.SELECTING;
    }

    public void confirmSchedule(LocalDateTime confirmedStartTime, LocalDateTime confirmedEndTime) {
        this.confirmedStartTime = confirmedStartTime;
        this.confirmedEndTime = confirmedEndTime;
        this.status = MeetingStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = MeetingStatus.CANCELED;
    }

    public void reopen() {
        this.status = MeetingStatus.GATHERING;
        this.topCandidateTimes = null;
        this.confirmedStartTime = null;
        this.confirmedEndTime = null;
    }
}
