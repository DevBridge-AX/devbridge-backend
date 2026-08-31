package com.devbridge.backend.domain.auth.service;

import com.devbridge.backend.domain.auth.dto.SendEmailRequest;
import com.devbridge.backend.domain.auth.dto.SignInRequest;
import com.devbridge.backend.domain.auth.dto.SignInResponse;
import com.devbridge.backend.domain.auth.dto.SignUpRequest;
import com.devbridge.backend.domain.auth.dto.VerifyEmailRequest;
import com.devbridge.backend.domain.auth.dto.VerifyHrRequest;
import com.devbridge.backend.domain.user.entity.ExternalHrEmployee;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.ExternalHrEmployeeRepository;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.user.service.UserInternalService;
import com.devbridge.backend.domain.workspace.service.WorkspaceAccessService;
import com.devbridge.backend.global.auth.jwt.JwtTokenProvider;
import com.devbridge.backend.global.common.exception.BusinessException;
import com.devbridge.backend.global.common.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final ExternalHrEmployeeRepository hrEmployeeRepository;
    // signUp의 신규 사용자 저장(:137)에만 사용한다. 조회는 UserInternalService를 거친다 —
    // 엔티티 생성(write)은 UserInternalService 설계 범위 밖으로 남겨둔 결정이다.
    private final UserRepository userRepository;
    private final UserInternalService userInternalService;
    private final WorkspaceAccessService workspaceAccessService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final JwtTokenProvider jwtTokenProvider;
    private final Environment environment;

    public String verifyHr(VerifyHrRequest request) {
        ExternalHrEmployee hr = hrEmployeeRepository
                .findByEmployeeIdAndName(request.employeeId(), request.name())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_HR_NOT_FOUND));

        if (!hr.isActive()) {
            throw new BusinessException(ErrorCode.AUTH_INACTIVE_EMPLOYEE);
        }

        return maskEmail(hr.getEmail());
    }

    public void validateAndSendEmailAuthCode(SendEmailRequest request) {
        hrEmployeeRepository.findByEmployeeIdAndEmail(request.employeeId(), request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_EMPLOYEE_EMAIL_MISMATCH));
        sendEmailAuthCode(request);
    }

    @Async
    public void sendEmailAuthCode(SendEmailRequest request) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        redisTemplate.opsForValue().set("auth:code:" + request.email(), code, 5, TimeUnit.MINUTES);

        try {
            Context context = new Context();
            context.setVariable("authCode", code);

            String htmlContent = templateEngine.process("mail-template", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(request.email());
            helper.setSubject("[SSAFY] DevBridge 인증번호입니다.");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_SEND_FAILED, "이메일 발송 중 오류가 발생했습니다.", e);
        }
    }

    public void verifyEmailAuthCode(VerifyEmailRequest request) {
        String key = "auth:code:" + request.email();
        String savedCode = redisTemplate.opsForValue().get(key);
        if (savedCode == null || !savedCode.equals(request.code())) {
            throw new BusinessException(ErrorCode.AUTH_CODE_MISMATCH);
        }

        redisTemplate.delete(key);
        redisTemplate.opsForValue().set("auth:verified:" + request.email(), "true", 30, TimeUnit.MINUTES);
    }

    @Transactional
    public void signUp(SignUpRequest request) {
        String verifiedKey = "auth:verified:" + request.email();

        if (!environment.acceptsProfiles(Profiles.of("dev", "local", "default"))) {
            String isVerified = redisTemplate.opsForValue().get(verifiedKey);
            if (isVerified == null || !isVerified.equals("true")) {
                throw new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
            }
        }

        ExternalHrEmployee hr = hrEmployeeRepository
                .findByEmployeeIdAndEmail(request.employeeId(), request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_VERIFICATION_FAILED));

        if (!hr.isActive()) {
            throw new BusinessException(ErrorCode.AUTH_VERIFICATION_FAILED);
        }

        String systemRole = "USER";   // 부서명, 직급명 코드로 변경 & 권한 부여
        if ("인사팀".equals(hr.getDepartment()) || "팀장".equals(hr.getPosition())) {
            systemRole = "HR_ADMIN"; // 케이스 추가 및 변경 예정
        }

        User user = User.builder()
                .employeeId(hr.getEmployeeId())
                .email(hr.getEmail())
                .name(hr.getName())
                .department(hr.getDepartment())
                .position(hr.getPosition())
                .passwordHash(passwordEncoder.encode(request.password()))
                .authProvider("LOCAL")
                .jobRole(request.jobRole())
                .systemRole(systemRole)
                .build();

        userRepository.save(user);
        log.info("Successfully saved user: " + user.getId());
        redisTemplate.delete(verifiedKey);
    }

    @Transactional(readOnly = true)
    public SignInResponse signIn(SignInRequest request) {
        User user = userInternalService.findByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_CREDENTIAL_MISMATCH));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_CREDENTIAL_MISMATCH);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmployeeId(), user.getSystemRole());
        String lastWorkspaceId = workspaceAccessService.findLastWorkspaceIdByEmployeeId(user.getEmployeeId());

        return SignInResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .lastWorkspaceId(lastWorkspaceId)
            .build();
    }

    public void logout(jakarta.servlet.http.HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            Long expiration = jwtTokenProvider.getExpiration(token);
            if (expiration > 0) {
                redisTemplate.opsForValue().set("blacklist:" + token, "logout", expiration, TimeUnit.MILLISECONDS);
            }
        }
    }

    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return email;
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        String masked = local.charAt(0) + "*".repeat(local.length() - 1);
        return masked + domain;
    }
}
