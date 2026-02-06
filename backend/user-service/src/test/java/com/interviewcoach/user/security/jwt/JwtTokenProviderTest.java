package com.interviewcoach.user.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET_KEY = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L;

    @BeforeEach
    void setUp() throws Exception {
        jwtTokenProvider = new JwtTokenProvider();

        setField(jwtTokenProvider, "secretKeyString", SECRET_KEY);
        setField(jwtTokenProvider, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        setField(jwtTokenProvider, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);

        jwtTokenProvider.init();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nested
    @DisplayName("토큰 생성")
    class CreateTokenTest {

        @Test
        @DisplayName("Access 토큰 생성 성공")
        void createAccessToken_Success() {
            // when
            String token = jwtTokenProvider.createAccessToken(1L, "user@test.com");

            // then
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Refresh 토큰 생성 성공")
        void createRefreshToken_Success() {
            // when
            String token = jwtTokenProvider.createRefreshToken(1L, "user@test.com");

            // then
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Access 토큰에 올바른 클레임 포함")
        void createAccessToken_ContainsCorrectClaims() {
            // when
            String token = jwtTokenProvider.createAccessToken(1L, "user@test.com");

            // then
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);

            assertThat(userId).isEqualTo(1L);
            assertThat(email).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("Access 토큰 타입은 access")
        void createAccessToken_HasAccessType() {
            // when
            String token = jwtTokenProvider.createAccessToken(1L, "user@test.com");

            // then
            assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
        }

        @Test
        @DisplayName("Refresh 토큰 타입은 refresh")
        void createRefreshToken_HasRefreshType() {
            // when
            String token = jwtTokenProvider.createRefreshToken(1L, "user@test.com");

            // then
            assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("토큰 검증")
    class ValidateTokenTest {

        @Test
        @DisplayName("유효한 토큰 검증 성공")
        void validateToken_ValidToken_ReturnsTrue() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L, "user@test.com");

            // when & then
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("잘못된 형식 토큰 검증 실패")
        void validateToken_MalformedToken_ReturnsFalse() {
            // when & then
            assertThat(jwtTokenProvider.validateToken("invalid-token")).isFalse();
        }

        @Test
        @DisplayName("빈 토큰 검증 실패")
        void validateToken_EmptyToken_ReturnsFalse() {
            // when & then
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰 검증 실패")
        void validateToken_ExpiredToken_ReturnsFalse() throws Exception {
            // given - 만료 시간을 0으로 설정
            setField(jwtTokenProvider, "accessTokenExpiration", 0L);
            jwtTokenProvider.init();

            String token = jwtTokenProvider.createAccessToken(1L, "user@test.com");

            // wait briefly for token to expire
            Thread.sleep(10);

            // when & then
            assertThat(jwtTokenProvider.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰 검증 실패")
        void validateToken_WrongKey_ReturnsFalse() {
            // given
            SecretKey otherKey = Keys.hmacShaKeyFor(
                    "another-secret-key-that-is-also-at-least-256-bits-long-yes".getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .subject("1")
                    .claim("email", "user@test.com")
                    .claim("type", "access")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(otherKey)
                    .compact();

            // when & then - SignatureException extends SecurityException (JJWT),
            // caught by java.lang.SecurityException catch block
            assertThat(jwtTokenProvider.validateToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("isRefreshToken")
    class IsRefreshTokenTest {

        @Test
        @DisplayName("잘못된 토큰으로 isRefreshToken 호출 시 false")
        void isRefreshToken_InvalidToken_ReturnsFalse() {
            assertThat(jwtTokenProvider.isRefreshToken("invalid")).isFalse();
        }
    }

    @Nested
    @DisplayName("토큰 파싱")
    class ParseTokenTest {

        @Test
        @DisplayName("토큰에서 사용자 ID 추출")
        void getUserIdFromToken_Success() {
            // given
            String token = jwtTokenProvider.createAccessToken(42L, "user@test.com");

            // when
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(userId).isEqualTo(42L);
        }

        @Test
        @DisplayName("토큰에서 이메일 추출")
        void getEmailFromToken_Success() {
            // given
            String token = jwtTokenProvider.createAccessToken(1L, "hello@world.com");

            // when
            String email = jwtTokenProvider.getEmailFromToken(token);

            // then
            assertThat(email).isEqualTo("hello@world.com");
        }
    }
}
