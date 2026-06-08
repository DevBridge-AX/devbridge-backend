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
import com.devbridge.backend.global.auth.jwt.JwtTokenProvider;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final JwtTokenProvider jwtTokenProvider;
    private final Environment environment;

    public String verifyHr(VerifyHrRequest request) {
        ExternalHrEmployee hr = hrEmployeeRepository
                .findByEmployeeIdAndName(request.employeeId(), request.name())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 사원 정보를 찾을 수 없습니다."));

        if (!hr.isActive()) {
            throw new IllegalArgumentException("비활성화된 사원입니다.");
        }

        return maskEmail(hr.getEmail());
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
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
        }
    }

    public void verifyEmailAuthCode(VerifyEmailRequest request) {
        String key = "auth:code:" + request.email();
        String savedCode = redisTemplate.opsForValue().get(key);
        if (savedCode == null || !savedCode.equals(request.code())) {
            throw new IllegalArgumentException("인증번호가 불일치하거나 만료되었습니다.");
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
                throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
            }
        }

        ExternalHrEmployee hr = hrEmployeeRepository
                .findByEmployeeIdAndEmail(request.employeeId(), request.email())
                .orElseThrow(() -> new IllegalArgumentException("인증 실패"));

        if (!hr.isActive()) {
            throw new IllegalArgumentException("인증 실패");
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
                .systemRole(systemRole)
                .build();

        userRepository.save(user);
        log.info("Successfully saved user: " + user.getId());
        redisTemplate.delete(verifiedKey);
    }

    @Transactional(readOnly = true)
    public SignInResponse signIn(SignInRequest request) {
        User user = userRepository.findByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("사번 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("사번 또는 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getSystemRole());
        return SignInResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
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
