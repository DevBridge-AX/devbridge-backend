package com.devbridge.backend.global.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JwtTokenProvider} 특성 테스트(characterization test).
 *
 * <p>이 클래스는 HTTP 인증({@code JwtAuthenticationFilter})과 WebSocket 핸드셰이크 인증
 * ({@code JwtHandshakeInterceptor}) 양쪽이 공통으로 의존하는 <b>인증의 뿌리</b>인데 테스트가 없었다.
 * 여기가 깨지면 REST와 WebSocket이 동시에 무너진다.
 *
 * <p>토큰의 클레임 구조(`subject`=employeeId, `role` 클레임, 2시간 만료)는 발급 후 프론트엔드로 전달되는
 * 계약이므로 값을 리터럴로 고정한다.
 */
class JwtTokenProviderTest {

    /** 테스트 전용 키. 운영 키와 무관하며 HS256 요구 길이(256bit)를 만족해야 한다. */
    private static final String SECRET =
            Base64.getEncoder().encodeToString("devbridge-test-secret-key-for-jwt-provider-0123456789".getBytes());

    private static final String EMPLOYEE_ID = "EMP001";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET);
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Test
    @DisplayName("createAccessToken_withEmployeeIdAndRole_setsSubjectAndRoleClaim")
    void createAccessToken_withEmployeeIdAndRole_setsSubjectAndRoleClaim() {
        String token = jwtTokenProvider.createAccessToken(EMPLOYEE_ID, "USER");

        var claims = Jwts.parser().verifyWith(key(SECRET)).build()
                .parseSignedClaims(token).getPayload();

        // subject는 사번(employeeId)이다. 06-19 작업으로 식별자가 사번으로 통일되었다.
        assertThat(claims.getSubject()).isEqualTo(EMPLOYEE_ID);
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    @DisplayName("createAccessToken_withValidInput_expiresInTwoHours")
    void createAccessToken_withValidInput_expiresInTwoHours() {
        String token = jwtTokenProvider.createAccessToken(EMPLOYEE_ID, "USER");

        var claims = Jwts.parser().verifyWith(key(SECRET)).build()
                .parseSignedClaims(token).getPayload();

        long lifetimeMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(lifetimeMillis).isEqualTo(2 * 60 * 60 * 1000L);
    }

    @Test
    @DisplayName("validateToken_withValidToken_returnsTrue")
    void validateToken_withValidToken_returnsTrue() {
        String token = jwtTokenProvider.createAccessToken(EMPLOYEE_ID, "USER");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken_withTokenSignedByOtherKey_returnsFalse")
    void validateToken_withTokenSignedByOtherKey_returnsFalse() {
        String otherSecret =
                Base64.getEncoder().encodeToString("another-secret-key-that-is-long-enough-0123456789".getBytes());
        String forged = Jwts.builder()
                .subject(EMPLOYEE_ID)
                .claim("role", "ADMIN")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key(otherSecret))
                .compact();

        // 서명 위조는 예외를 던지지 않고 false를 반환한다.
        assertThat(jwtTokenProvider.validateToken(forged)).isFalse();
    }

    @Test
    @DisplayName("validateToken_withExpiredToken_returnsFalse")
    void validateToken_withExpiredToken_returnsFalse() {
        String expired = Jwts.builder()
                .subject(EMPLOYEE_ID)
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key(SECRET))
                .compact();

        assertThat(jwtTokenProvider.validateToken(expired)).isFalse();
    }

    @Test
    @DisplayName("validateToken_withMalformedToken_returnsFalse")
    void validateToken_withMalformedToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken_withNullToken_returnsFalse")
    void validateToken_withNullToken_returnsFalse() {
        // null도 예외 없이 false로 처리된다(IllegalArgumentException을 내부에서 catch).
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("getAuthentication_withValidToken_grantsRolePrefixedAuthority")
    void getAuthentication_withValidToken_grantsRolePrefixedAuthority() {
        String token = jwtTokenProvider.createAccessToken(EMPLOYEE_ID, "USER");

        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        assertThat(authentication.getPrincipal()).isEqualTo(EMPLOYEE_ID);
        assertThat(authentication.getCredentials()).isEqualTo(token);
        // Spring Security 규약상 ROLE_ 접두사가 붙는다. SecurityConfig의 hasRole("INTERNAL") 등과 대응한다.
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("extractEmployeeId_withValidToken_returnsSubject")
    void extractEmployeeId_withValidToken_returnsSubject() {
        String token = jwtTokenProvider.createAccessToken(EMPLOYEE_ID, "USER");

        assertThat(jwtTokenProvider.extractEmployeeId(token)).isEqualTo(EMPLOYEE_ID);
    }

    @Test
    @DisplayName("extractEmployeeId_withInvalidToken_throwsException")
    void extractEmployeeId_withInvalidToken_throwsException() {
        // validateToken과 달리 예외를 삼키지 않는다. 호출 전에 반드시 validateToken을 거쳐야 한다.
        assertThatThrownBy(() -> jwtTokenProvider.extractEmployeeId("not-a-jwt"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("getExpiration_withValidToken_returnsRemainingMillis")
    void getExpiration_withValidToken_returnsRemainingMillis() {
        String token = jwtTokenProvider.createAccessToken(EMPLOYEE_ID, "USER");

        Long remaining = jwtTokenProvider.getExpiration(token);

        // 로그아웃 시 이 값이 Redis 블랙리스트 TTL로 쓰인다.
        assertThat(remaining).isPositive().isLessThanOrEqualTo(2 * 60 * 60 * 1000L);
    }

    @Test
    @DisplayName("getExpiration_withExpiredToken_returnsZero")
    void getExpiration_withExpiredToken_returnsZero() {
        String expired = Jwts.builder()
                .subject(EMPLOYEE_ID)
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key(SECRET))
                .compact();

        // 만료 토큰은 파싱 단계에서 예외가 나고 0으로 폴백된다 → 블랙리스트에 등록되지 않는다.
        assertThat(jwtTokenProvider.getExpiration(expired)).isZero();
    }

    @Test
    @DisplayName("getExpiration_withMalformedToken_returnsZero")
    void getExpiration_withMalformedToken_returnsZero() {
        assertThat(jwtTokenProvider.getExpiration("not-a-jwt")).isZero();
    }
}
