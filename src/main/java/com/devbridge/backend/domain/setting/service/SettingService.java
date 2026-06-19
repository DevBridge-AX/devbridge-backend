package com.devbridge.backend.domain.setting.service;

import com.devbridge.backend.domain.setting.dto.ProfileResponse;
import com.devbridge.backend.domain.setting.dto.UpdatePasswordRequest;
import com.devbridge.backend.domain.setting.dto.UpdateProfileRequest;
import com.devbridge.backend.domain.user.dto.UserResponse;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getUser(String employeeId) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. employeeId: " + employeeId));

        return UserResponse.builder()
                .id(user.getId())
                .employeeId(user.getEmployeeId())
                .email(user.getEmail())
                .name(user.getName())
                .department(user.getDepartment())
                .position(user.getPosition())
                .jobRole(user.getJobRole() != null ? user.getJobRole().name() : null)
                .systemRole(user.getSystemRole())
                .build();
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String employeeId) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. employeeId: " + employeeId));

        return new ProfileResponse(
                user.getId(),
                user.getEmployeeId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getPosition(),
                user.getJobRole() != null ? user.getJobRole().name() : null
        );
    }

    @Transactional
    public void updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id: " + userId));

        user.updateProfile(request.name(), request.department(), request.position(), request.jobRole());
    }

    @Transactional
    public void changePassword(String userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id: " + userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        String newHash = passwordEncoder.encode(request.newPassword());
        user.changePassword(newHash);
    }

    @Transactional(readOnly = true)
    public void verifyPassword(String userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id: " + userId));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
    }
}