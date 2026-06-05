package com.devbridge.backend.domain.auth.service;

import com.devbridge.backend.domain.auth.dto.SignInRequest;
import com.devbridge.backend.domain.auth.dto.SignInResponse;
import com.devbridge.backend.domain.auth.dto.SignUpRequest;
import com.devbridge.backend.domain.auth.dto.SendEmailRequest;
import com.devbridge.backend.domain.auth.dto.VerifyEmailRequest;
import com.devbridge.backend.domain.user.entity.ExternalHrEmployee;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.ExternalHrEmployeeRepository;
import com.devbridge.backend.domain.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.thymeleaf.TemplateEngine;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import com.devbridge.backend.global.auth.jwt.JwtTokenProvider;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

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


    @Transactional
    public void signUp(SignUpRequest request) {
        String verifiedKey = "auth:verified:" + request.email();
        String isVerified = redisTemplate.opsForValue().get(verifiedKey);
        if (isVerified == null || !isVerified.equals("true")) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다");
        }

        ExternalHrEmployee hrEmployee = hrEmployeeRepository.findByEmployeeIdAndEmail(request.employeeId(), request.email())
                .orElseThrow(() -> new IllegalArgumentException("인증 실패"));

        if (!hrEmployee.isActive()) {
            throw new IllegalArgumentException("인증 실패");
        }

        String systemRole = "USER";     // 인사팀 - HR?Peoples?로 변경 / 직급은 B(리더) / C(C레벨) -> 1,2,3 구분하여 특정 이상 권한 부여
        if ("인사팀".equals(hrEmployee.getDepartment()) || "팀장".equals(hrEmployee.getPosition())) {
            systemRole = "HR_ADMIN"; // 필요에 따라 분류
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = User.builder()
                .employeeId(hrEmployee.getEmployeeId())
                .email(hrEmployee.getEmail())
                .name(hrEmployee.getName())
                .department(hrEmployee.getDepartment())
                .position(hrEmployee.getPosition())
                .passwordHash(passwordHash)
                .authProvider("LOCAL")
                .systemRole(systemRole)
                .build();

        userRepository.save(user);

        redisTemplate.delete(verifiedKey);
    }

    @Async
    public void sendEmailAuthCode(SendEmailRequest request) {
        ExternalHrEmployee hrEmployee = hrEmployeeRepository.findByEmployeeIdAndEmail(request.employeeId(), request.email())
                .orElseThrow(() -> new IllegalArgumentException("인증 실패"));

        if (!hrEmployee.isActive()) {
            throw new IllegalArgumentException("인증 실패");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        redisTemplate.opsForValue().set("auth:email:" + request.email(), code, 3, TimeUnit.MINUTES);

        try {
            Context context = new org.thymeleaf.context.Context();
            context.setVariable("authCode", code);

            String htmlContent = templateEngine.process("email-auth", context);

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
        String key = "auth:email:" + request.email();
        String savedCode = redisTemplate.opsForValue().get(key);
        if (savedCode == null || !savedCode.equals(request.code())) {
            throw new IllegalArgumentException("인증번호가 불일치하거나 만료되었습니다");
        }

        redisTemplate.delete(key);
        redisTemplate.opsForValue().set("auth:verified:" + request.email(), "true", 30, TimeUnit.MINUTES);
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
}
