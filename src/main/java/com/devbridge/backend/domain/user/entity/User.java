package com.devbridge.backend.domain.user.entity;

import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "USERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE USERS SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id; // 사용자 ID (JPA UUID)

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    private String employeeId; // 사번 (고유 식별자)

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email; // 이메일

    @Column(name = "password_hash", length = 255)
    private String passwordHash; // 비밀번호 해시 (SSO 연동 시 NULL 허용)

    @Builder.Default
    @Column(name = "auth_provider", nullable = false, length = 50)
    private String authProvider = "LOCAL"; // 인증 제공자 (LOCAL, SSO 등)

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 사용자명

    @Column(name = "department", length = 100)
    private String department; // 부서

    @Column(name = "position", length = 100)
    private String position; // 직급

    @Enumerated(EnumType.STRING)
    @Column(name = "job_role", length = 50)
    private JobRole jobRole; // 직무 (PLANNER, DEVELOPER, QA, DESIGNER, OPERATOR, NEWCOMER)

    @Column(name = "system_role", nullable = false, length = 50)
    private String systemRole; // 권한 (ADMIN, USER)

    public void updateProfile(String name, String department, String position, JobRole jobRole) {
        if (name != null) this.name = name;
        if (department != null) this.department = department;
        if (position != null) this.position = position;
        if (jobRole != null) this.jobRole = jobRole;
    }

    public void changePassword(String newPasswordHash) {
        if (newPasswordHash != null) this.passwordHash = newPasswordHash;
    }
}
