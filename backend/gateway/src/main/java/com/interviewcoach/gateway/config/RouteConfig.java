package com.interviewcoach.gateway.config;

import com.interviewcoach.gateway.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public RouteConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service routes
                .route("user-service-auth", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://localhost:8081"))
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://localhost:8081"))

                // Question Service routes
                .route("question-service-jd", r -> r
                        .path("/api/v1/jd/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://localhost:8082"))
                .route("question-service-questions", r -> r
                        .path("/api/v1/questions/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://localhost:8082"))

                // Interview Service routes
                .route("interview-service", r -> r
                        .path("/api/v1/interviews/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://localhost:8083"))

                // Feedback Service routes
                .route("feedback-service", r -> r
                        .path("/api/v1/feedback/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://localhost:8084"))
                .route("statistics-service", r -> r
                        .path("/api/v1/statistics/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("http://localhost:8084"))

                .build();
    }
}
