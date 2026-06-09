package com.devbridge.backend.domain.user.repository;

import com.devbridge.backend.domain.user.entity.ExternalHrEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExternalHrEmployeeRepository extends JpaRepository<ExternalHrEmployee, String> {
    Optional<ExternalHrEmployee> findByEmployeeIdAndEmail(String employeeId, String email);
    Optional<ExternalHrEmployee> findByEmployeeIdAndName(String employeeId, String name);
}
