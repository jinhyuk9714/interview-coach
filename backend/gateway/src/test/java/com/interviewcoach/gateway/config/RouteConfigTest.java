package com.interviewcoach.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
@DisplayName("RouteConfig 통합 테스트")
class RouteConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    @DisplayName("RouteLocator Bean이 정상 등록")
    void routeLocator_IsLoaded() {
        assertThat(routeLocator).isNotNull();
    }

    @Test
    @DisplayName("7개 라우트가 정의되어 있음")
    void routeLocator_Has7Routes() {
        List<String> routeIds = routeLocator.getRoutes()
                .map(route -> route.getId())
                .collectList()
                .block();

        assertThat(routeIds).isNotNull();
        assertThat(routeIds).hasSize(7);
        assertThat(routeIds).containsExactlyInAnyOrder(
                "user-service-auth",
                "user-service",
                "question-service-jd",
                "question-service-questions",
                "interview-service",
                "feedback-service",
                "statistics-service"
        );
    }
}
