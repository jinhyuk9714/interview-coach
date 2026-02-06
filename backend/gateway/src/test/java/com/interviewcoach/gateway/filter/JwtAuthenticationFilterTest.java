package com.interviewcoach.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("JwtAuthenticationFilter 단위 테스트")
class JwtAuthenticationFilterTest {

    private static final String SECRET_KEY = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha";
    private JwtAuthenticationFilter filter;
    private GatewayFilterChain chain;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(SECRET_KEY);
        chain = mock(GatewayFilterChain.class);
        secretKey = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        given(chain.filter(any())).willReturn(Mono.empty());
    }

    private String createValidToken(Long userId, String email) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();
    }

    private String createExpiredToken() {
        return Jwts.builder()
                .subject("1")
                .claim("email", "user@test.com")
                .claim("type", "access")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(secretKey)
                .compact();
    }

    @Nested
    @DisplayName("공개 경로")
    class PublicPathTest {

        @Test
        @DisplayName("/api/v1/auth/** 경로는 인증 없이 통과")
        void authPath_PassesWithoutToken() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/auth/login")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("/actuator/** 경로는 인증 없이 통과")
        void actuatorPath_PassesWithoutToken() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/actuator/health")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
        }
    }

    @Nested
    @DisplayName("인증 필요 경로")
    class ProtectedPathTest {

        @Test
        @DisplayName("유효한 Bearer 토큰으로 요청 - 통과 + X-User-Id 헤더 추가")
        void validToken_PassesAndAddsHeaders() {
            // given
            String token = createValidToken(42L, "user@test.com");
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/interviews")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
        }

        @Test
        @DisplayName("토큰 없이 보호된 경로 요청 - 401 Unauthorized")
        void noToken_Returns401() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/interviews")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("잘못된 토큰 형식 - 401 Unauthorized")
        void malformedToken_Returns401() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/interviews")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("만료된 토큰 - 401 Unauthorized")
        void expiredToken_Returns401() {
            // given
            String token = createExpiredToken();
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/interviews")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("SSE 쿼리 파라미터 토큰으로 요청 - 통과")
        void queryParamToken_Passes() {
            // given
            String token = createValidToken(1L, "user@test.com");
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/feedback/session/1/stream?token=" + token)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Authorization 헤더 형식")
    class AuthHeaderFormatTest {

        @Test
        @DisplayName("Bearer 접두사 없는 Authorization 헤더 - 401")
        void noBearerPrefix_Returns401() {
            // given
            String token = createValidToken(1L, "user@test.com");
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/interviews")
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            Mono<Void> result = filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain);

            // then
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
