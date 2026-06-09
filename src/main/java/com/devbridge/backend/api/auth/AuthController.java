package com.devbridge.backend.api.auth;

import com.devbridge.backend.domain.auth.dto.SignInRequest;
import com.devbridge.backend.domain.auth.dto.SignInResponse;
import com.devbridge.backend.domain.auth.dto.SignUpRequest;
import com.devbridge.backend.domain.auth.dto.SendEmailRequest;
import com.devbridge.backend.domain.auth.dto.VerifyEmailRequest;
import com.devbridge.backend.domain.auth.dto.VerifyHrRequest;
import com.devbridge.backend.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthAPI {

    private final AuthService authService;

    @Override
    public ResponseEntity<String> verifyHr(VerifyHrRequest request) {
        String maskedEmail = authService.verifyHr(request);
        return ResponseEntity.ok(maskedEmail);
    }

    @Override
    public ResponseEntity<Void> signUp(SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<SignInResponse> signIn(SignInRequest request) {
        SignInResponse response = authService.signIn(request);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> sendEmailAuthCode(SendEmailRequest request) {
        authService.validateAndSendEmailAuthCode(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> verifyEmailAuthCode(VerifyEmailRequest request) {
        authService.verifyEmailAuthCode(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok().build();
    }
}