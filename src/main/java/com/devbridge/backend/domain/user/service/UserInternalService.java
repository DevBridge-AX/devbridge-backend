package com.devbridge.backend.domain.user.service;

import com.devbridge.backend.domain.user.dto.UserLookupResponse;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.global.common.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 타 도메인이 {@code user} 도메인 데이터를 조회할 때 거치는 유일한 창구.
 * {@link UserRepository}를 직접 주입받는 도메인 서비스는 이 클래스를 통해서만 접근해야 한다.
 *
 * <p><b>존재하지 않을 때 예외를 던지지 않고 {@link Optional}을 반환한다.</b> 호출부마다 "사용자
 * 없음"에 대해 서로 다른 예외 타입·메시지를 사용하며(예: {@code AuthService}는 "사번 또는
 * 비밀번호가 일치하지 않습니다.", {@code MeetingReferenceService}는 "해당 사용자가 존재하지
 * 않습니다: {employeeId}"), 이는 이미 특성 테스트로 고정되어 있다. 이 서비스가 대신 예외를
 * 던지면 호출부가 자기 도메인에 맞는 메시지를 낼 수 없다.
 *
 * <p><b>{@link User} 엔티티를 그대로 반환한다.</b> DTO로 평탄화하지 않는 이유는, 호출부 다수가
 * 조회 결과를 {@code @ManyToOne} 연관관계(예: {@code Task.requester}, {@code WorkspaceMember.user})에
 * 그대로 대입해야 하기 때문이다. {@code backend.md}의 "타 도메인 코드를 import하여 사용하는 것은
 * 허용" 규칙에 따라 엔티티 타입 자체를 참조하는 것은 문제가 없다 — 금지되는 것은
 * {@link UserRepository}를 직접 주입받아 쿼리하는 것이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInternalService {

    private final UserRepository userRepository;

    /**
     * 외부(AI 엔진 등) 연동용 API — {@code InternalUserController}가 사용한다.
     * 기존 계약(존재하지 않으면 {@link UserNotFoundException} throw)을 그대로 유지한다.
     */
    public UserLookupResponse lookupUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return new UserLookupResponse(user.getId());
    }

    public Optional<User> findByEmployeeId(String employeeId) {
        return userRepository.findByEmployeeId(employeeId);
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
