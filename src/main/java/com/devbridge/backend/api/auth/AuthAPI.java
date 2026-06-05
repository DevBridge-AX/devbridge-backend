package com.devbridge.backend.api.auth;

import com.devbridge.backend.domain.auth.dto.SignInRequest;
import com.devbridge.backend.domain.auth.dto.SignInResponse;
import com.devbridge.backend.domain.auth.dto.SignUpRequest;
import com.devbridge.backend.domain.auth.dto.SendEmailRequest;
import com.devbridge.backend.domain.auth.dto.VerifyEmailRequest;
import com.devbridge.backend.domain.auth.dto.VerifyHrRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Authentication")
@RequestMapping("/api/auth")
public interface AuthAPI {

    @Operation(summary = "Verify HR Employee")
    @PostMapping("/verify-hr")
    ResponseEntity<String> verifyHr(@Valid @RequestBody VerifyHrRequest request);

    @Operation(summary = "SignUp")
    @PostMapping("/signup")
    ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest request);

    @Operation(summary = "SignIn")
    @PostMapping("/signin")
    ResponseEntity<SignInResponse> signIn(@Valid @RequestBody SignInRequest request);

    @Operation(summary = "SendEmailAuthCode")
    @PostMapping("/email/send")
    ResponseEntity<Void> sendEmailAuthCode(@Valid @RequestBody SendEmailRequest request);

    @Operation(summary = "VerifyEmailAuthCode")
    @PostMapping("/email/verify")
    ResponseEntity<Void> verifyEmailAuthCode(@Valid @RequestBody VerifyEmailRequest request);

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request);
}