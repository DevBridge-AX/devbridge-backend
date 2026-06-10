package com.devbridge.backend.domain.schedule.entity;

import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

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

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private MeetingStatus status;

    @Column(name = "confirmed_start_time")
    private LocalDateTime confirmedStartTime;

    @Column(name = "confirmed_end_time")
    private LocalDateTime confirmedEndTime;
}
