package com.devbridge.backend.global.config.websocket;

import com.devbridge.backend.domain.user.entity.JobRole;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.global.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JwtHandshakeInterceptor} 특성 테스트(characterization test).
 *
 * <p>기존 {@code WebSocketAuthTest}는 실제 서버를 띄우는 통합 테스트라
 * "유효/무효/토큰없음" 3가지 경로만 확인한다. 아래 분기들은 그 테스트로 검증되지 않는다.
 *
 * <ul>
 *   <li>블랙리스트(로그아웃)된 토큰으로 재접속하는 경우</li>
 *   <li>Redis 장애 시 블랙리스트 검사를 우회하는 경로</li>
 *   <li>토큰은 유효하나 사용자가 DB에 없을 때의 {@code NEWCOMER} 폴백</li>
 * </ul>
 *
 * <p>핵심 구조: <b>인증 실패해도 {@code beforeHandshake}는 항상 {@code true}를 반환</b>한다.
 * 즉 핸드셰이크는 성립시키고, {@code employeeId} attribute 부재를 근거로
 * {@code ChatWebSocketHandler}가 close(4401)로 끊는다. 이 2단 구조가 바뀌면 인증이 뚫린다.
 */
@ExtendWith(MockitoExtension.class)
class JwtHandshakeInterceptorTest {

    private static final String TOKEN = "valid-token";
    private static final String EMPLOYEE_ID = "EMP001";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    private JwtHandshakeInterceptor interceptor;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        interceptor = new JwtHandshakeInterceptor(jwtTokenProvider, redisTemplate, userRepository);
        attributes = new HashMap<>();
    }

    private void givenUri(String uri) {
        lenient().when(request.getURI()).thenReturn(URI.create(uri));
    }

    private boolean handshake() {
        return interceptor.beforeHandshake(request, response, wsHandler, attributes);
    }

    @Test
    @DisplayName("beforeHandshake_withValidToken_putsEmployeeIdAndJobRoleAttributes")
    void beforeHandshake_withValidToken_putsEmployeeIdAndJobRoleAttributes() {
        givenUri("ws://localhost:8080/ws/chat?token=" + TOKEN);
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:" + TOKEN)).thenReturn(false);
        when(jwtTokenProvider.extractEmployeeId(TOKEN)).thenReturn(EMPLOYEE_ID);
        when(userRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(
                Optional.of(User.builder().employeeId(EMPLOYEE_ID).jobRole(JobRole.DEVELOPER).build()));

        assertThat(handshake()).isTrue();
        assertThat(attributes).containsEntry("employeeId", EMPLOYEE_ID);
        // jobRole은 소문자로 변환되어 저장된다. ChatWebSocketHandler가 이 값을 FastAPI role로 전달한다.
        assertThat(attributes).containsEntry("jobRole", "developer");
    }

    @Test
    @DisplayName("beforeHandshake_withValidTokenButUnknownUser_fallsBackToNewcomerRole")
    void beforeHandshake_withValidTokenButUnknownUser_fallsBackToNewcomerRole() {
        givenUri("ws://localhost:8080/ws/chat?token=" + TOKEN);
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:" + TOKEN)).thenReturn(false);
        when(jwtTokenProvider.extractEmployeeId(TOKEN)).thenReturn(EMPLOYEE_ID);
        when(userRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertThat(handshake()).isTrue();
        // 사용자가 DB에 없어도 연결은 허용되며 jobRole만 newcomer로 폴백된다.
        assertThat(attributes).containsEntry("employeeId", EMPLOYEE_ID);
        assertThat(attributes).containsEntry("jobRole", "newcomer");
    }

    @Test
    @DisplayName("beforeHandshake_withBlacklistedToken_returnsTrueWithoutAttributes")
    void beforeHandshake_withBlacklistedToken_returnsTrueWithoutAttributes() {
        givenUri("ws://localhost:8080/ws/chat?token=" + TOKEN);
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:" + TOKEN)).thenReturn(true);

        // 로그아웃된 토큰: 핸드셰이크는 성립하지만 attribute가 비어 있어
        // ChatWebSocketHandler가 close(4401)로 끊는다.
        assertThat(handshake()).isTrue();
        assertThat(attributes).isEmpty();
        verify(jwtTokenProvider, never()).extractEmployeeId(anyString());
    }

    @Test
    @DisplayName("beforeHandshake_whenRedisUnavailable_treatsTokenAsNotBlacklisted")
    void beforeHandshake_whenRedisUnavailable_treatsTokenAsNotBlacklisted() {
        givenUri("ws://localhost:8080/ws/chat?token=" + TOKEN);
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:" + TOKEN))
                .thenThrow(new RedisConnectionFailureException("redis down"));
        when(jwtTokenProvider.extractEmployeeId(TOKEN)).thenReturn(EMPLOYEE_ID);
        when(userRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(
                Optional.of(User.builder().employeeId(EMPLOYEE_ID).jobRole(JobRole.QA).build()));

        assertThat(handshake()).isTrue();
        // Redis 장애 시 블랙리스트 검사를 우회하고 연결을 허용한다(가용성 우선).
        // HTTP 필터(JwtAuthenticationFilter)와 동일한 정책이다.
        assertThat(attributes).containsEntry("employeeId", EMPLOYEE_ID);
    }

    @Test
    @DisplayName("beforeHandshake_withInvalidToken_returnsTrueWithoutAttributes")
    void beforeHandshake_withInvalidToken_returnsTrueWithoutAttributes() {
        givenUri("ws://localhost:8080/ws/chat?token=invalid");
        when(jwtTokenProvider.validateToken("invalid")).thenReturn(false);

        assertThat(handshake()).isTrue();
        assertThat(attributes).isEmpty();
    }

    @Test
    @DisplayName("beforeHandshake_withoutTokenParam_returnsTrueWithoutAttributes")
    void beforeHandshake_withoutTokenParam_returnsTrueWithoutAttributes() {
        givenUri("ws://localhost:8080/ws/chat");

        assertThat(handshake()).isTrue();
        assertThat(attributes).isEmpty();
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("beforeHandshake_withDifferentQueryParamName_ignoresToken")
    void beforeHandshake_withDifferentQueryParamName_ignoresToken() {
        // 파라미터명은 프론트엔드와 공유하는 불변 계약(WebSocketContract.TOKEN_QUERY_PARAM)이다.
        // 'token' 외의 이름으로 보내면 인증되지 않는다.
        givenUri("ws://localhost:8080/ws/chat?accessToken=" + TOKEN);

        assertThat(handshake()).isTrue();
        assertThat(attributes).isEmpty();
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }
}
