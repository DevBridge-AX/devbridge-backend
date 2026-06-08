package com.devbridge.backend.api.setting;

import com.devbridge.backend.domain.setting.dto.ProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Setting")
@RequestMapping("/api/settings")
public interface SettingAPI {

    @Operation(summary = "Get my profile")
    @GetMapping("/profile")
    ResponseEntity<ProfileResponse> getProfile(String userId);
}
