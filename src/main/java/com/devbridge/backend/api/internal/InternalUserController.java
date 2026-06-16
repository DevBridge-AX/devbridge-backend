package com.devbridge.backend.api.internal;

import com.devbridge.backend.domain.user.dto.UserLookupResponse;
import com.devbridge.backend.domain.user.service.UserInternalService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class InternalUserController {

    private final UserInternalService userInternalService;

    @GetMapping("/internal/users/lookup")
    public ResponseEntity<UserLookupResponse> lookupUserByEmail(
            @RequestParam("email") @NotBlank @Email String email
    ) {
        UserLookupResponse response = userInternalService.lookupUserByEmail(email);
        return ResponseEntity.ok(response);
    }
}
