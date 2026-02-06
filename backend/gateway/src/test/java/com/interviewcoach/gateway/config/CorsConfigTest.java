package com.interviewcoach.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secret=test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha",
                "services.user-service.url=http://localhost:8081",
                "services.question-service.url=http://localhost:8082",
                "services.interview-service.url=http://localhost:8083",
                "services.feedback-service.url=http://localhost:8084"
        }
)
@ActiveProfiles("test")
@DisplayName("CorsConfig 통합 테스트")
class CorsConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("CORS preflight 요청 - 허용된 Origin")
    void corsPreflightAllowedOrigin() {
        webTestClient.options()
                .uri("/api/v1/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().exists("Access-Control-Allow-Methods")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("CORS preflight 요청 - 허용되지 않은 Origin")
    void corsPreflightDisallowedOrigin() {
        webTestClient.options()
                .uri("/api/v1/auth/login")
                .header("Origin", "http://evil.com")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectHeader().doesNotExist("Access-Control-Allow-Origin");
    }
}
