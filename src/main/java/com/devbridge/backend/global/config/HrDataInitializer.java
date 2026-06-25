package com.devbridge.backend.global.config;

import com.devbridge.backend.domain.user.entity.ExternalHrEmployee;
import com.devbridge.backend.domain.user.repository.ExternalHrEmployeeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HrDataInitializer implements ApplicationRunner {

    private final ExternalHrEmployeeRepository repository;

    public HrDataInitializer(ExternalHrEmployeeRepository repository) {
        this.repository = repository;
    }

    private static final List<ExternalHrEmployee> SEED_DATA = List.of(
            ExternalHrEmployee.builder()
                    .employeeId("EMP001").email("admin@company.com")
                    .name("김현수").department("개발팀").position("Backend Developer").isActive(true).build(),
            ExternalHrEmployee.builder()
                    .employeeId("EMP002").email("user@company.com")
                    .name("이원빈").department("개발팀").position("사원").isActive(true).build(),
            ExternalHrEmployee.builder()
                    .employeeId("EMP003").email("choie000208@gmail.com")
                    .name("최형수").department("기획팀").position("팀장").isActive(true).build(),
            ExternalHrEmployee.builder()
                    .employeeId("EMP004").email("sam000208@naver.com")
                    .name("최펭수").department("개발팀").position("대리").isActive(true).build(),
            ExternalHrEmployee.builder()
                    .employeeId("EMP005").email("designer@company.com")
                    .name("전우석").department("디자인팀").position("디자이너").isActive(true).build(),
            ExternalHrEmployee.builder()
                    .employeeId("EMP006").email("operator@company.com")
                    .name("김서연").department("운영팀").position("대리").isActive(true).build(),
            ExternalHrEmployee.builder()
                    .employeeId("EMP007").email("newcomer@company.com")
                    .name("김정윤").department("인사팀").position("사원").isActive(true).build(),
            ExternalHrEmployee.builder()
                    .employeeId("EMP008").email("julie019@naver.com")
                    .name("김양평").department("마케팅팀").position("대리").isActive(true).build()
    );

    @Override
    public void run(ApplicationArguments args) {
        for (ExternalHrEmployee seed : SEED_DATA) {
            repository.findById(seed.getEmployeeId()).ifPresentOrElse(
                    existing -> {
                        // 인코딩 문제 등으로 name이 비었을 경우 복구
                        if (existing.getName() == null || existing.getName().isBlank()) {
                            repository.save(ExternalHrEmployee.builder()
                                    .employeeId(existing.getEmployeeId())
                                    .email(existing.getEmail())
                                    .name(seed.getName())
                                    .department(existing.getDepartment())
                                    .position(existing.getPosition())
                                    .isActive(existing.isActive())
                                    .build());
                        }
                    },
                    () -> repository.save(seed)
            );
        }
    }
}
