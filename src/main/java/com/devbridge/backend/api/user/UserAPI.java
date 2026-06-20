package com.devbridge.backend.api.user;

import com.devbridge.backend.domain.setting.dto.UpdatePasswordRequest;
import com.devbridge.backend.domain.setting.dto.UpdateProfileRequest;
import com.devbridge.backend.domain.setting.dto.VerifyPasswordRequest;
import com.devbridge.backend.domain.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "User", description = "사용자 관리 API 명세")
@RequestMapping("/api/users")
public interface UserAPI {

    @Operation(summary = "내 정보 조회", description = "인증된 사용자 본인의 정보를 조회합니다.")
    @GetMapping("/me")
    ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal String employeeId);

    @Operation(summary = "사용자 조회", description = "사용자 ID에 해당하는 사용자 정보를 상세 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<UserResponse> getUser(@PathVariable("id") String id);

    @Operation(summary = "사용자 프로필 수정", description = "JWT 인증된 본인의 프로필 정보를 수정합니다.")
    @PatchMapping("/me/profile")
    ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody UpdateProfileRequest request
    );

    @Operation(summary = "비밀번호 변경", description = "JWT 인증된 본인의 비밀번호를 변경합니다.")
    @PutMapping("/me/password")
    ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody UpdatePasswordRequest request
    );

    @Operation(summary = "현재 비밀번호 검증", description = "JWT 인증된 본인의 현재 비밀번호를 검증합니다.")
    @PostMapping("/me/password/verify")
    ResponseEntity<Void> verifyPassword(
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody VerifyPasswordRequest request
    );
}
