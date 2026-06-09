package com.devbridge.backend.api.setting;

import com.devbridge.backend.domain.setting.dto.ProfileResponse;
import com.devbridge.backend.domain.setting.dto.UpdateProfileRequest;
import com.devbridge.backend.domain.setting.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SettingController implements SettingAPI {

    private final SettingService settingService;

    @Override
    public ResponseEntity<ProfileResponse> getProfile(
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(settingService.getProfile(userId));
    }

    @Override
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody UpdateProfileRequest request
    ) {
        settingService.updateProfile(userId, request);
        return ResponseEntity.ok().build();
    }
}
