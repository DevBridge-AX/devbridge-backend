package com.devbridge.backend.domain.setting.service;

import com.devbridge.backend.domain.setting.dto.ProfileResponse;
import com.devbridge.backend.domain.setting.dto.UpdatePasswordRequest;
import com.devbridge.backend.domain.setting.dto.UpdateProfileRequest;
import com.devbridge.backend.domain.user.dto.UserResponse;
import com.devbridge.backend.domain.user.entity.JobRole;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.service.UserInternalService;
import com.devbridge.backend.global.common.exception.BusinessException;
import com.devbridge.backend.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * {@link SettingService} 특성 테스트(characterization test).
 *
 * <p>"올바른 동작"이 아니라 <b>현재 동작</b>을 그대로 고정하는 것이 목적이다.
 * {@code UserRepository} 직접 참조는 {@link UserInternalService}로 치환 완료했다(C-3, refactor/setting).
 * raw throw 2건(비밀번호 검증 실패)은 보안 관련 경로라 특히 중요하다.
 */
@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

    private static final String EMPLOYEE_ID = "EMP001";
    private static final String CURRENT_PASSWORD = "Current1!";
    private static final String CURRENT_HASH = "current-encoded";
    private static final String NEW_PASSWORD = "NewPassword1!";
    private static final String NEW_HASH = "new-encoded";

    @Mock
    private UserInternalService userInternalService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SettingService settingService;

    @BeforeEach
    void setUp() {
        settingService = new SettingService(userInternalService, passwordEncoder);
    }

    private User user() {
        return user(JobRole.DEVELOPER);
    }

    private User user(JobRole jobRole) {
        return User.builder()
                .id("user-1")
                .employeeId(EMPLOYEE_ID)
                .email("hong@devbridge.com")
                .passwordHash(CURRENT_HASH)
                .name("홍길동")
                .department("개발팀")
                .position("사원")
                .jobRole(jobRole)
                .systemRole("USER")
                .build();
    }

    private void givenUser(User user) {
        when(userInternalService.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(user));
    }

    private void givenUserNotFound() {
        when(userInternalService.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("getUser_withExistingEmployee_returnsMappedUserResponse")
        void getUser_withExistingEmployee_returnsMappedUserResponse() {
            givenUser(user());

            UserResponse response = settingService.getUser(EMPLOYEE_ID);

            assertThat(response.getId()).isEqualTo("user-1");
            assertThat(response.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(response.getEmail()).isEqualTo("hong@devbridge.com");
            assertThat(response.getName()).isEqualTo("홍길동");
            assertThat(response.getDepartment()).isEqualTo("개발팀");
            assertThat(response.getPosition()).isEqualTo("사원");
            assertThat(response.getJobRole()).isEqualTo("DEVELOPER");
            assertThat(response.getSystemRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("getUser_withNullJobRole_returnsNullJobRoleName")
        void getUser_withNullJobRole_returnsNullJobRoleName() {
            givenUser(user(null));

            UserResponse response = settingService.getUser(EMPLOYEE_ID);

            assertThat(response.getJobRole()).isNull();
        }

        @Test
        @DisplayName("getUser_withUnknownEmployee_throwsIllegalArgumentException")
        void getUser_withUnknownEmployee_throwsIllegalArgumentException() {
            givenUserNotFound();

            assertThatThrownBy(() -> settingService.getUser(EMPLOYEE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("사용자를 찾을 수 없습니다. employeeId: " + EMPLOYEE_ID)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SETTING_USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("getProfile_withExistingEmployee_returnsMappedProfileResponse")
        void getProfile_withExistingEmployee_returnsMappedProfileResponse() {
            givenUser(user());

            ProfileResponse response = settingService.getProfile(EMPLOYEE_ID);

            assertThat(response).isEqualTo(new ProfileResponse(
                    "user-1", EMPLOYEE_ID, "홍길동", "hong@devbridge.com", "개발팀", "사원", "DEVELOPER"));
        }

        @Test
        @DisplayName("getProfile_withUnknownEmployee_throwsIllegalArgumentException")
        void getProfile_withUnknownEmployee_throwsIllegalArgumentException() {
            givenUserNotFound();

            assertThatThrownBy(() -> settingService.getProfile(EMPLOYEE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("사용자를 찾을 수 없습니다. employeeId: " + EMPLOYEE_ID)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SETTING_USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("updateProfile_withAllFields_overwritesUserFields")
        void updateProfile_withAllFields_overwritesUserFields() {
            User user = user();
            givenUser(user);

            settingService.updateProfile(EMPLOYEE_ID,
                    new UpdateProfileRequest("김철수", "기획팀", "팀장", JobRole.PLANNER));

            assertThat(user.getName()).isEqualTo("김철수");
            assertThat(user.getDepartment()).isEqualTo("기획팀");
            assertThat(user.getPosition()).isEqualTo("팀장");
            assertThat(user.getJobRole()).isEqualTo(JobRole.PLANNER);
        }

        @Test
        @DisplayName("updateProfile_withNullFields_keepsExistingValues")
        void updateProfile_withNullFields_keepsExistingValues() {
            // 현재 구현은 null 필드를 건너뛰는 부분 업데이트다 — 전체 덮어쓰기가 아니다.
            User user = user();
            givenUser(user);

            settingService.updateProfile(EMPLOYEE_ID, new UpdateProfileRequest(null, null, null, null));

            assertThat(user.getName()).isEqualTo("홍길동");
            assertThat(user.getDepartment()).isEqualTo("개발팀");
            assertThat(user.getPosition()).isEqualTo("사원");
            assertThat(user.getJobRole()).isEqualTo(JobRole.DEVELOPER);
        }

        @Test
        @DisplayName("updateProfile_withUnknownEmployee_throwsIllegalArgumentException")
        void updateProfile_withUnknownEmployee_throwsIllegalArgumentException() {
            givenUserNotFound();

            assertThatThrownBy(() -> settingService.updateProfile(EMPLOYEE_ID,
                    new UpdateProfileRequest("김철수", null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("사용자를 찾을 수 없습니다. employeeId: " + EMPLOYEE_ID)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SETTING_USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("changePassword_withCorrectCurrentPassword_updatesPasswordHash")
        void changePassword_withCorrectCurrentPassword_updatesPasswordHash() {
            User user = user();
            givenUser(user);
            when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH)).thenReturn(true);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

            settingService.changePassword(EMPLOYEE_ID, new UpdatePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD));

            assertThat(user.getPasswordHash()).isEqualTo(NEW_HASH);
        }

        @Test
        @DisplayName("changePassword_withWrongCurrentPassword_throwsIllegalArgumentException")
        void changePassword_withWrongCurrentPassword_throwsIllegalArgumentException() {
            User user = user();
            givenUser(user);
            when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH)).thenReturn(false);

            assertThatThrownBy(() -> settingService.changePassword(
                    EMPLOYEE_ID, new UpdatePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("현재 비밀번호가 일치하지 않습니다.")
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SETTING_PASSWORD_MISMATCH));

            // 검증 실패 시 새 비밀번호로 인코딩하지 않는다.
            assertThat(user.getPasswordHash()).isEqualTo(CURRENT_HASH);
        }

        @Test
        @DisplayName("changePassword_withUnknownEmployee_throwsIllegalArgumentException")
        void changePassword_withUnknownEmployee_throwsIllegalArgumentException() {
            givenUserNotFound();

            assertThatThrownBy(() -> settingService.changePassword(
                    EMPLOYEE_ID, new UpdatePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("사용자를 찾을 수 없습니다. employeeId: " + EMPLOYEE_ID)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SETTING_USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("verifyPassword")
    class VerifyPassword {

        @Test
        @DisplayName("verifyPassword_withCorrectPassword_doesNotThrow")
        void verifyPassword_withCorrectPassword_doesNotThrow() {
            givenUser(user());
            when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH)).thenReturn(true);

            assertThatCode(() -> settingService.verifyPassword(EMPLOYEE_ID, CURRENT_PASSWORD))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("verifyPassword_withWrongPassword_throwsIllegalArgumentException")
        void verifyPassword_withWrongPassword_throwsIllegalArgumentException() {
            givenUser(user());
            when(passwordEncoder.matches("wrong-password", CURRENT_HASH)).thenReturn(false);

            assertThatThrownBy(() -> settingService.verifyPassword(EMPLOYEE_ID, "wrong-password"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("현재 비밀번호가 일치하지 않습니다.")
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SETTING_PASSWORD_MISMATCH));
        }

        @Test
        @DisplayName("verifyPassword_withUnknownEmployee_throwsIllegalArgumentException")
        void verifyPassword_withUnknownEmployee_throwsIllegalArgumentException() {
            givenUserNotFound();

            assertThatThrownBy(() -> settingService.verifyPassword(EMPLOYEE_ID, CURRENT_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("사용자를 찾을 수 없습니다. employeeId: " + EMPLOYEE_ID)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SETTING_USER_NOT_FOUND));
        }
    }
}
