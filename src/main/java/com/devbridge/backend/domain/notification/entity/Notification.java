package com.devbridge.backend.domain.notification.entity;

import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "NOTIFICATIONS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 알림 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 수신자 ID

    @Column(name = "type", nullable = false, length = 50)
    private String type; // 알림 종류

    @Column(name = "reference_id", length = 36)
    private String referenceId; // 클릭 시 이동할 타겟 ID

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false; // 읽음 여부

    public void markAsRead() {
        this.isRead = true;
    }
}
