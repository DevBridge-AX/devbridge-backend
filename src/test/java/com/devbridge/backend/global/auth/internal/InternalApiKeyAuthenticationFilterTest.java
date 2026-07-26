package com.devbridge.backend.global.auth.internal;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InternalApiKeyAuthenticationFilter} 특성 테스트(characterization test).
 *
 * <p>이 필터는 {@code /internal/**} 경로를 보호하며, <b>AI 엔진(FastAPI)이 백엔드를 호출할 때 쓰는 유일한 인증 수단</b>이다.
 * ({@code SecurityConfig}에서 {@code /internal/**}는 {@code hasRole("INTERNAL")}로 제한된다.)
 * 레포 간 연동의 관문인데 테스트가 없었다.
 */
@ExtendWith(MockitoExtension.class)
class InternalApiKeyAuthenticationFilterTest {

    private static final String HEADER = "X-Internal-Api-Key";
    private static final String API_KEY = "internal-secret-key";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private InternalApiKeyAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalApiKeyAuthenticationFilter(API_KEY);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal_withMatchingApiKey_grantsInternalRole")
    void doFilterInternal_withMatchingApiKey_grantsInternalRole() throws Exception {
        when(request.getHeader(HEADER)).thenReturn(API_KEY);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo("INTERNAL_SERVICE");
        // SecurityConfig의 hasRole("INTERNAL")과 대응하는 권한명이다.
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_INTERNAL");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal_withWrongApiKey_doesNotAuthenticateButContinuesChain")
    void doFilterInternal_withWrongApiKey_doesNotAuthenticateButContinuesChain() throws Exception {
        when(request.getHeader(HEADER)).thenReturn("wrong-key");

        filter.doFilter(request, response, filterChain);

        // 401을 직접 던지지 않고 체인을 계속 진행한다. 차단은 SecurityConfig의 hasRole에 위임된다.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal_withoutApiKeyHeader_doesNotAuthenticate")
    void doFilterInternal_withoutApiKeyHeader_doesNotAuthenticate() throws Exception {
        when(request.getHeader(HEADER)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal_withApiKeyDifferingByCase_doesNotAuthenticate")
    void doFilterInternal_withApiKeyDifferingByCase_doesNotAuthenticate() throws Exception {
        // equals 비교이므로 대소문자가 다르면 인증되지 않는다.
        when(request.getHeader(HEADER)).thenReturn(API_KEY.toUpperCase());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
