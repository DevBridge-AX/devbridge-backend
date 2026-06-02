package com.devbridge.backend.domain.schedule.entity;

import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SCHEDULE_ATTENDEES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ScheduleAttendee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 참석자 매핑 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private SmartSchedule schedule; // 일정 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 참석 대상자 ID

    @Column(name = "available_times", columnDefinition = "json")
    private String availableTimes; // 유저가 제출한 가능 시간 목록

    @Builder.Default
    @Column(name = "response_status", nullable = false, length = 50)
    private String responseStatus = "PENDING"; // PENDING, ACCEPTED, DECLINED
}
