package com.devbridge.backend.domain.setting.service;

import com.devbridge.backend.domain.setting.dto.ProfileResponse;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return new ProfileResponse(
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getPosition()
        );
    }
}
