package com.devbridge.backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "temp_external_hr_employees")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalHrEmployee {

    @Id
    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
