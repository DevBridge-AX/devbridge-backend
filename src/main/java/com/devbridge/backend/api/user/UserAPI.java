package com.devbridge.backend.api.user;

import com.devbridge.backend.domain.setting.dto.UpdatePasswordRequest;
import com.devbridge.backend.domain.setting.dto.UpdateProfileRequest;
import com.devbridge.backend.domain.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "User", description = "사용자 관리 API 명세")
@RequestMapping("/api/users")
public interface UserAPI {

    @Operation(summary = "사용자 조회", description = "사용자 ID에 해당하는 사용자 정보를 상세 조회합니다.")
    @GetMapping("/{id}")
    ResponseEntity<UserResponse> getUser(@PathVariable("id") String id);

    @Operation(summary = "사용자 프로필 수정")
    @PatchMapping("/{userId}/profile")
    ResponseEntity<Void> updateProfile(
            @PathVariable("userId") String userId,
            @Valid @RequestBody UpdateProfileRequest request
    );

    @Operation(summary = "비밀번호 변경")
    @PutMapping("/{userId}/password")
    ResponseEntity<Void> updatePassword(
            @PathVariable("userId") String userId,
            @Valid @RequestBody UpdatePasswordRequest request
    );
}
