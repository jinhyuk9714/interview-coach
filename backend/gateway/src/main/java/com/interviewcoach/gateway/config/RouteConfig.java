package com.interviewcoach.gateway.config;

import com.interviewcoach.gateway.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${services.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${services.question-service.url:http://localhost:8082}")
    private String questionServiceUrl;

    @Value("${services.interview-service.url:http://localhost:8083}")
    private String interviewServiceUrl;

    @Value("${services.feedback-service.url:http://localhost:8084}")
    private String feedbackServiceUrl;

    public RouteConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service routes
                .route("user-service-auth", r -> r
                        .path("/api/v1/auth/**")
                        .uri(userServiceUrl))
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri(userServiceUrl))

                // Question Service routes
                .route("question-service-jd", r -> r
                        .path("/api/v1/jd/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri(questionServiceUrl))
                .route("question-service-questions", r -> r
                        .path("/api/v1/questions/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri(questionServiceUrl))

                // Interview Service routes
                .route("interview-service", r -> r
                        .path("/api/v1/interviews/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri(interviewServiceUrl))

                // Feedback Service routes
                .route("feedback-service", r -> r
                        .path("/api/v1/feedback/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri(feedbackServiceUrl))
                .route("statistics-service", r -> r
                        .path("/api/v1/statistics/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri(feedbackServiceUrl))

                .build();
    }
}
