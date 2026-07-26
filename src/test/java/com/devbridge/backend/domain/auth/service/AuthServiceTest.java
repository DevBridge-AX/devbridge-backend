package com.devbridge.backend.domain.auth.service;

import com.devbridge.backend.domain.auth.dto.SendEmailRequest;
import com.devbridge.backend.domain.auth.dto.SignInRequest;
import com.devbridge.backend.domain.auth.dto.SignInResponse;
import com.devbridge.backend.domain.auth.dto.SignUpRequest;
import com.devbridge.backend.domain.auth.dto.VerifyEmailRequest;
import com.devbridge.backend.domain.auth.dto.VerifyHrRequest;
import com.devbridge.backend.domain.user.entity.ExternalHrEmployee;
import com.devbridge.backend.domain.user.entity.JobRole;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.ExternalHrEmployeeRepository;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.service.WorkspaceAccessService;
import com.devbridge.backend.global.auth.jwt.JwtTokenProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthService} 특성 테스트(characterization test).
 *
 * <p>"올바른 동작"이 아니라 <b>현재 동작</b>을 그대로 고정하는 것이 목적이다.
 * `auth` 도메인은 이 레포에서 유일하게 테스트가 0건이면서 raw throw를 6건 보유한 도메인이라,
 * 도메인 경계 정리(`UserRepository` 직접 참조 제거)와 예외 체계 도입 전에 안전망이 필요하다.
 *
 * <p><b>예외 타입이 곧 HTTP 상태코드다.</b> `GlobalExceptionHandler` 기준으로
 * {@link IllegalArgumentException}은 400, {@link RuntimeException}은 500으로 매핑된다.
 * 따라서 타입과 메시지를 리터럴로 고정해 상태코드 회귀를 감지한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMPLOYEE_ID = "EMP001";
    private static final String EMAIL = "hong@devbridge.com";
    private static final String NAME = "홍길동";
    private static final String RAW_PASSWORD = "Password1!";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private ExternalHrEmployeeRepository hrEmployeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Environment environment;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                hrEmployeeRepository,
                userRepository,
                workspaceAccessService,
                passwordEncoder,
                redisTemplate,
                mailSender,
                templateEngine,
                jwtTokenProvider,
                environment
        );

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private ExternalHrEmployee hrEmployee(boolean active) {
        return hrEmployee(active, "개발팀", "사원");
    }

    private ExternalHrEmployee hrEmployee(boolean active, String department, String position) {
        return ExternalHrEmployee.builder()
                .employeeId(EMPLOYEE_ID)
                .email(EMAIL)
                .name(NAME)
                .department(department)
                .position(position)
                .isActive(active)
                .build();
    }

    @Nested
    @DisplayName("verifyHr")
    class VerifyHr {

        @Test
        @DisplayName("verifyHr_withActiveEmployee_returnsMaskedEmail")
        void verifyHr_withActiveEmployee_returnsMaskedEmail() {
            when(hrEmployeeRepository.findByEmployeeIdAndName(EMPLOYEE_ID, NAME))
                    .thenReturn(Optional.of(hrEmployee(true)));

            String masked = authService.verifyHr(new VerifyHrRequest(EMPLOYEE_ID, NAME));

            // local part "hong" → 첫 글자만 남기고 나머지를 *로 대체한다.
            assertThat(masked).isEqualTo("h***@devbridge.com");
        }

        @Test
        @DisplayName("verifyHr_withSingleCharLocalPart_returnsEmailUnmasked")
        void verifyHr_withSingleCharLocalPart_returnsEmailUnmasked() {
            ExternalHrEmployee hr = ExternalHrEmployee.builder()
                    .employeeId(EMPLOYEE_ID).email("a@devbridge.com").name(NAME).isActive(true).build();
            when(hrEmployeeRepository.findByEmployeeIdAndName(EMPLOYEE_ID, NAME))
                    .thenReturn(Optional.of(hr));

            // 현재 구현은 '@' 위치가 1 이하이면 마스킹을 건너뛰고 원본을 그대로 반환한다.
            assertThat(authService.verifyHr(new VerifyHrRequest(EMPLOYEE_ID, NAME)))
                    .isEqualTo("a@devbridge.com");
        }

        @Test
        @DisplayName("verifyHr_withUnknownEmployee_throwsIllegalArgumentException")
        void verifyHr_withUnknownEmployee_throwsIllegalArgumentException() {
            when(hrEmployeeRepository.findByEmployeeIdAndName(EMPLOYEE_ID, NAME))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyHr(new VerifyHrRequest(EMPLOYEE_ID, NAME)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("일치하는 사원 정보를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("verifyHr_withInactiveEmployee_throwsIllegalArgumentException")
        void verifyHr_withInactiveEmployee_throwsIllegalArgumentException() {
            when(hrEmployeeRepository.findByEmployeeIdAndName(EMPLOYEE_ID, NAME))
                    .thenReturn(Optional.of(hrEmployee(false)));

            assertThatThrownBy(() -> authService.verifyHr(new VerifyHrRequest(EMPLOYEE_ID, NAME)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비활성화된 사원입니다.");
        }
    }

    @Nested
    @DisplayName("이메일 인증코드 발송")
    class SendAuthCode {

        @Test
        @DisplayName("validateAndSendEmailAuthCode_withMismatchedEmail_throwsIllegalArgumentException")
        void validateAndSendEmailAuthCode_withMismatchedEmail_throwsIllegalArgumentException() {
            when(hrEmployeeRepository.findByEmployeeIdAndEmail(EMPLOYEE_ID, EMAIL))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.validateAndSendEmailAuthCode(
                    new SendEmailRequest(EMPLOYEE_ID, EMAIL)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사번과 등록된 이메일 정보가 일치하지 않습니다.");

            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("sendEmailAuthCode_withValidRequest_storesSixDigitCodeForFiveMinutes")
        void sendEmailAuthCode_withValidRequest_storesSixDigitCodeForFiveMinutes() {
            when(templateEngine.process(eq("mail-template"), any(Context.class))).thenReturn("<html/>");
            when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));

            authService.sendEmailAuthCode(new SendEmailRequest(EMPLOYEE_ID, EMAIL));

            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(
                    eq("auth:code:" + EMAIL), codeCaptor.capture(), eq(5L), eq(TimeUnit.MINUTES));

            assertThat(codeCaptor.getValue()).hasSize(6).containsOnlyDigits();
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("sendEmailAuthCode_whenMailFails_throwsRuntimeExceptionMappedTo500")
        void sendEmailAuthCode_whenMailFails_throwsRuntimeExceptionMappedTo500() throws Exception {
            MimeMessage mimeMessage = org.mockito.Mockito.mock(MimeMessage.class);
            when(templateEngine.process(eq("mail-template"), any(Context.class))).thenReturn("<html/>");
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MessagingException("발송 실패")).when(mimeMessage).setRecipient(any(), any());

            // 현재 구현은 RuntimeException을 던지며, GlobalExceptionHandler의 handleGeneral에 걸려
            // HTTP 500 + "서버 오류가 발생했습니다."로 응답한다.
            // 이메일 발송 실패는 서버 장애가 아니므로 상태코드가 부적절하나, 여기서는 현재 동작을 고정한다.
            assertThatThrownBy(() -> authService.sendEmailAuthCode(new SendEmailRequest(EMPLOYEE_ID, EMAIL)))
                    .isInstanceOf(RuntimeException.class)
                    .isNotInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이메일 발송 중 오류가 발생했습니다.")
                    .hasCauseInstanceOf(MessagingException.class);

            // 인증코드는 메일 발송 이전에 이미 저장된다(발송 실패해도 Redis에 남는다).
            verify(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        }
    }

    @Nested
    @DisplayName("verifyEmailAuthCode")
    class VerifyEmailAuthCode {

        @Test
        @DisplayName("verifyEmailAuthCode_withMatchingCode_marksEmailVerifiedForThirtyMinutes")
        void verifyEmailAuthCode_withMatchingCode_marksEmailVerifiedForThirtyMinutes() {
            when(valueOperations.get("auth:code:" + EMAIL)).thenReturn("123456");

            authService.verifyEmailAuthCode(new VerifyEmailRequest(EMAIL, "123456"));

            verify(redisTemplate).delete("auth:code:" + EMAIL);
            verify(valueOperations).set("auth:verified:" + EMAIL, "true", 30, TimeUnit.MINUTES);
        }

        @Test
        @DisplayName("verifyEmailAuthCode_withExpiredCode_throwsIllegalArgumentException")
        void verifyEmailAuthCode_withExpiredCode_throwsIllegalArgumentException() {
            when(valueOperations.get("auth:code:" + EMAIL)).thenReturn(null);

            assertThatThrownBy(() -> authService.verifyEmailAuthCode(new VerifyEmailRequest(EMAIL, "123456")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증번호가 불일치하거나 만료되었습니다.");

            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("verifyEmailAuthCode_withWrongCode_throwsIllegalArgumentException")
        void verifyEmailAuthCode_withWrongCode_throwsIllegalArgumentException() {
            when(valueOperations.get("auth:code:" + EMAIL)).thenReturn("123456");

            assertThatThrownBy(() -> authService.verifyEmailAuthCode(new VerifyEmailRequest(EMAIL, "999999")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증번호가 불일치하거나 만료되었습니다.");
        }
    }

    @Nested
    @DisplayName("signUp")
    class SignUp {

        private SignUpRequest request() {
            return new SignUpRequest(EMPLOYEE_ID, EMAIL, RAW_PASSWORD, JobRole.DEVELOPER);
        }

        private void givenNonLocalProfile() {
            when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        }

        @Test
        @DisplayName("signUp_withVerifiedEmail_savesUserWithEncodedPasswordAndDeletesVerifiedKey")
        void signUp_withVerifiedEmail_savesUserWithEncodedPasswordAndDeletesVerifiedKey() {
            givenNonLocalProfile();
            when(valueOperations.get("auth:verified:" + EMAIL)).thenReturn("true");
            when(hrEmployeeRepository.findByEmployeeIdAndEmail(EMPLOYEE_ID, EMAIL))
                    .thenReturn(Optional.of(hrEmployee(true)));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            authService.signUp(request());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User saved = captor.getValue();
            assertThat(saved.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(saved.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
            assertThat(saved.getAuthProvider()).isEqualTo("LOCAL");
            assertThat(saved.getSystemRole()).isEqualTo("USER");
            assertThat(saved.getJobRole()).isEqualTo(JobRole.DEVELOPER);
            verify(redisTemplate).delete("auth:verified:" + EMAIL);
        }

        @Test
        @DisplayName("signUp_withLocalProfile_skipsEmailVerification")
        void signUp_withLocalProfile_skipsEmailVerification() {
            // dev/local/default 프로필에서는 이메일 인증 검증을 통째로 건너뛴다.
            when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
            when(hrEmployeeRepository.findByEmployeeIdAndEmail(EMPLOYEE_ID, EMAIL))
                    .thenReturn(Optional.of(hrEmployee(true)));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            authService.signUp(request());

            verify(userRepository).save(any(User.class));
            verify(valueOperations, never()).get("auth:verified:" + EMAIL);
        }

        @Test
        @DisplayName("signUp_withHrDepartment_grantsHrAdminRole")
        void signUp_withHrDepartment_grantsHrAdminRole() {
            givenNonLocalProfile();
            when(valueOperations.get("auth:verified:" + EMAIL)).thenReturn("true");
            when(hrEmployeeRepository.findByEmployeeIdAndEmail(EMPLOYEE_ID, EMAIL))
                    .thenReturn(Optional.of(hrEmployee(true, "인사팀", "사원")));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            authService.signUp(request());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getSystemRole()).isEqualTo("HR_ADMIN");
        }

        @Test
        @DisplayName("signUp_withTeamLeadPosition_grantsHrAdminRole")
        void signUp_withTeamLeadPosition_grantsHrAdminRole() {
            givenNonLocalProfile();
            when(valueOperations.get("auth:verified:" + EMAIL)).thenReturn("true");
            when(hrEmployeeRepository.findByEmployeeIdAndEmail(EMPLOYEE_ID, EMAIL))
                    .thenReturn(Optional.of(hrEmployee(true, "개발팀", "팀장")));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            authService.signUp(request());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getSystemRole()).isEqualTo("HR_ADMIN");
        }

        @Test
        @DisplayName("signUp_withoutEmailVerification_throwsIllegalArgumentException")
        void signUp_withoutEmailVerification_throwsIllegalArgumentException() {
            givenNonLocalProfile();
            when(valueOperations.get("auth:verified:" + EMAIL)).thenReturn(null);

            assertThatThrownBy(() -> authService.signUp(request()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이메일 인증이 완료되지 않았습니다.");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("signUp_withUnknownEmployee_throwsIllegalArgumentException")
        void signUp_withUnknownEmployee_throwsIllegalArgumentException() {
            givenNonLocalProfile();
            when(valueOperations.get("auth:verified:" + EMAIL)).thenReturn("true");
            when(hrEmployeeRepository.findByEmployeeIdAndEmail(EMPLOYEE_ID, EMAIL))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.signUp(request()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증 실패");
        }

        @Test
        @DisplayName("signUp_withInactiveEmployee_throwsIllegalArgumentException")
        void signUp_withInactiveEmployee_throwsIllegalArgumentException() {
            givenNonLocalProfile();
            when(valueOperations.get("auth:verified:" + EMAIL)).thenReturn("true");
            when(hrEmployeeRepository.findByEmployeeIdAndEmail(EMPLOYEE_ID, EMAIL))
                    .thenReturn(Optional.of(hrEmployee(false)));

            // 미존재와 비활성이 동일한 메시지를 사용한다(사용자 열거 방지).
            assertThatThrownBy(() -> authService.signUp(request()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증 실패");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("signIn")
    class SignIn {

        private SignInRequest request() {
            return SignInRequest.builder().employeeId(EMPLOYEE_ID).password(RAW_PASSWORD).build();
        }

        private User user() {
            return User.builder()
                    .id("USER-001")
                    .employeeId(EMPLOYEE_ID)
                    .passwordHash(ENCODED_PASSWORD)
                    .systemRole("USER")
                    .build();
        }

        @Test
        @DisplayName("signIn_withValidCredentials_returnsBearerTokenAndLastWorkspaceId")
        void signIn_withValidCredentials_returnsBearerTokenAndLastWorkspaceId() {
            when(userRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(user()));
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtTokenProvider.createAccessToken(EMPLOYEE_ID, "USER")).thenReturn("access-token");
            when(workspaceAccessService.findLastWorkspaceIdByEmployeeId(EMPLOYEE_ID)).thenReturn("WS-001");

            SignInResponse response = authService.signIn(request());

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getLastWorkspaceId()).isEqualTo("WS-001");
        }

        @Test
        @DisplayName("signIn_withUnknownEmployeeId_throwsIllegalArgumentException")
        void signIn_withUnknownEmployeeId_throwsIllegalArgumentException() {
            when(userRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.signIn(request()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사번 또는 비밀번호가 일치하지 않습니다.");
        }

        @Test
        @DisplayName("signIn_withWrongPassword_throwsSameMessageAsUnknownEmployeeId")
        void signIn_withWrongPassword_throwsSameMessageAsUnknownEmployeeId() {
            when(userRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(user()));
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

            // 사번 미존재와 비밀번호 불일치가 동일한 메시지를 반환한다(사용자 열거 방지).
            assertThatThrownBy(() -> authService.signIn(request()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사번 또는 비밀번호가 일치하지 않습니다.");

            verify(jwtTokenProvider, never()).createAccessToken(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Mock
        private HttpServletRequest httpRequest;

        @Test
        @DisplayName("logout_withBearerToken_blacklistsTokenUntilExpiration")
        void logout_withBearerToken_blacklistsTokenUntilExpiration() {
            when(httpRequest.getHeader("Authorization")).thenReturn("Bearer sample-token");
            when(jwtTokenProvider.getExpiration("sample-token")).thenReturn(60_000L);

            authService.logout(httpRequest);

            verify(valueOperations).set(
                    "blacklist:sample-token", "logout", 60_000L, TimeUnit.MILLISECONDS);
        }

        @Test
        @DisplayName("logout_withExpiredToken_doesNotBlacklist")
        void logout_withExpiredToken_doesNotBlacklist() {
            when(httpRequest.getHeader("Authorization")).thenReturn("Bearer expired-token");
            when(jwtTokenProvider.getExpiration("expired-token")).thenReturn(0L);

            authService.logout(httpRequest);

            verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("logout_withoutAuthorizationHeader_doesNothing")
        void logout_withoutAuthorizationHeader_doesNothing() {
            when(httpRequest.getHeader("Authorization")).thenReturn(null);

            authService.logout(httpRequest);

            verify(jwtTokenProvider, never()).getExpiration(anyString());
            verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("logout_withNonBearerHeader_doesNothing")
        void logout_withNonBearerHeader_doesNothing() {
            when(httpRequest.getHeader("Authorization")).thenReturn("Basic abcdef");

            authService.logout(httpRequest);

            verify(jwtTokenProvider, never()).getExpiration(anyString());
        }
    }
}
