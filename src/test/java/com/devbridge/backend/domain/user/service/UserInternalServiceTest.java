package com.devbridge.backend.domain.user.service;

import com.devbridge.backend.domain.user.dto.UserLookupResponse;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.global.common.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * {@link UserInternalService} 특성 테스트(characterization test).
 *
 * <p>이 서비스는 도메인 간 {@code user} 조회의 유일한 창구다({@code UserInternalService} javadoc 참고).
 * {@code findByEmployeeId}/{@code findById}/{@code findByEmail}은 존재하지 않을 때 예외를 던지지
 * 않고 {@link Optional#empty()}를 반환해야 한다 — 호출부(CHS·KHS 각 도메인)가 자기 예외를
 * 던질 수 있어야 하기 때문이다. {@code lookupUserByEmail}은 외부(AI 엔진) 연동용 기존 계약이라
 * 예외를 던지는 동작을 그대로 유지해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class UserInternalServiceTest {

    private static final String EMPLOYEE_ID = "EMP001";
    private static final String USER_ID = "user-1";
    private static final String EMAIL = "hong@devbridge.com";

    @Mock
    private UserRepository userRepository;

    private UserInternalService userInternalService;

    @BeforeEach
    void setUp() {
        userInternalService = new UserInternalService(userRepository);
    }

    private User user() {
        return User.builder().id(USER_ID).employeeId(EMPLOYEE_ID).email(EMAIL).build();
    }

    @Nested
    @DisplayName("lookupUserByEmail")
    class LookupUserByEmail {

        @Test
        @DisplayName("lookupUserByEmail_withExistingEmail_returnsUserId")
        void lookupUserByEmail_withExistingEmail_returnsUserId() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));

            UserLookupResponse response = userInternalService.lookupUserByEmail(EMAIL);

            assertThat(response.userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("lookupUserByEmail_withUnknownEmail_throwsUserNotFoundException")
        void lookupUserByEmail_withUnknownEmail_throwsUserNotFoundException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userInternalService.lookupUserByEmail(EMAIL))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with email: " + EMAIL);
        }
    }

    @Nested
    @DisplayName("findByEmployeeId")
    class FindByEmployeeId {

        @Test
        @DisplayName("findByEmployeeId_withExistingEmployee_returnsUser")
        void findByEmployeeId_withExistingEmployee_returnsUser() {
            when(userRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(user()));

            assertThat(userInternalService.findByEmployeeId(EMPLOYEE_ID))
                    .isPresent()
                    .get()
                    .extracting(User::getId)
                    .isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("findByEmployeeId_withUnknownEmployee_returnsEmpty")
        void findByEmployeeId_withUnknownEmployee_returnsEmpty() {
            when(userRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

            assertThat(userInternalService.findByEmployeeId(EMPLOYEE_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("findById_withExistingId_returnsUser")
        void findById_withExistingId_returnsUser() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));

            assertThat(userInternalService.findById(USER_ID))
                    .isPresent()
                    .get()
                    .extracting(User::getEmployeeId)
                    .isEqualTo(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("findById_withUnknownId_returnsEmpty")
        void findById_withUnknownId_returnsEmpty() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThat(userInternalService.findById(USER_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("findByEmail_withExistingEmail_returnsUser")
        void findByEmail_withExistingEmail_returnsUser() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));

            assertThat(userInternalService.findByEmail(EMAIL))
                    .isPresent()
                    .get()
                    .extracting(User::getId)
                    .isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("findByEmail_withUnknownEmail_returnsEmpty")
        void findByEmail_withUnknownEmail_returnsEmpty() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThat(userInternalService.findByEmail(EMAIL)).isEmpty();
        }
    }
}
