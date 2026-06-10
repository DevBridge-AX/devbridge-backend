package com.devbridge.backend.api.user;

import com.devbridge.backend.domain.setting.dto.UpdatePasswordRequest;
import com.devbridge.backend.domain.setting.dto.UpdateProfileRequest;
import com.devbridge.backend.domain.setting.dto.VerifyPasswordRequest;
import com.devbridge.backend.domain.setting.service.SettingService;
import com.devbridge.backend.domain.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserAPI {

    private final SettingService settingService;

    @Override
    public ResponseEntity<UserResponse> getUser(String id) {
        // TODO: 더미 데이터 제거 후 수정
        UserResponse response = UserResponse.builder()
                .id(id)
                .employeeId("EMP-12345")
                .email("user@devbridge.com")
                .name("홍길동")
                .department("개발팀")
                .position("선임")
                .systemRole("USER")
                .build();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> updateProfile(String userId, UpdateProfileRequest request) {
        settingService.updateProfile(userId, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updatePassword(String userId, UpdatePasswordRequest request) {
        settingService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> verifyPassword(String userId, VerifyPasswordRequest request) {
        settingService.verifyPassword(userId, request.password());
        return ResponseEntity.ok().build();
    }
}
