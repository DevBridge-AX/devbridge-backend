package com.devbridge.backend.global.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JwtAuthenticationFilter} 특성 테스트(characterization test).
 *
 * <p>모든 HTTP 요청이 통과하는 인증 필터인데 테스트가 없었다.
 * 특히 <b>인증 실패 시에도 필터 체인을 계속 진행</b>하는 구조라(401을 직접 던지지 않음),
 * 인가 판단은 이후 Security 설정에 위임된다. 이 동작이 바뀌면 전체 API 접근 제어가 흔들린다.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "valid-token";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken("EMP001", TOKEN, List.of());
    }

    @Test
    @DisplayName("doFilterInternal_withValidBearerToken_setsAuthenticationAndContinuesChain")
    void doFilterInternal_withValidBearerToken_setsAuthenticationAndContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:" + TOKEN)).thenReturn(false);
        when(jwtTokenProvider.getAuthentication(TOKEN)).thenReturn(authentication());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal_withBlacklistedToken_doesNotAuthenticateButContinuesChain")
    void doFilterInternal_withBlacklistedToken_doesNotAuthenticateButContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:" + TOKEN)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        // 로그아웃된 토큰은 인증되지 않지만, 필터는 401을 던지지 않고 체인을 계속 진행한다.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).getAuthentication(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal_whenRedisUnavailable_treatsTokenAsNotBlacklisted")
    void doFilterInternal_whenRedisUnavailable_treatsTokenAsNotBlacklisted() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:" + TOKEN))
                .thenThrow(new RedisConnectionFailureException("redis down"));
        when(jwtTokenProvider.getAuthentication(TOKEN)).thenReturn(authentication());

        filter.doFilter(request, response, filterChain);

        // Redis 장애 시 블랙리스트 검사를 우회하고 인증을 허용한다(가용성 우선 설계).
        // 즉 Redis가 죽으면 로그아웃된 토큰도 다시 통과한다.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal_withInvalidToken_doesNotAuthenticate")
    void doFilterInternal_withInvalidToken_doesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(redisTemplate, never()).hasKey(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal_withoutAuthorizationHeader_doesNotAuthenticate")
    void doFilterInternal_withoutAuthorizationHeader_doesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal_withNonBearerScheme_doesNotAuthenticate")
    void doFilterInternal_withNonBearerScheme_doesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abcdef");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal_withWebSocketPath_stillRunsWithoutAuthentication")
    void doFilterInternal_withWebSocketPath_stillRunsWithoutAuthentication() throws Exception {
        // shouldNotFilter를 재정의하지 않으므로 /ws/** 요청에도 이 필터가 붙는다.
        // 다만 WebSocket 핸드셰이크는 Authorization 헤더 없이 쿼리 파라미터로 토큰을 보내므로
        // 여기서는 인증되지 않고 통과하며, 실제 인증은 JwtHandshakeInterceptor가 담당한다.
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
