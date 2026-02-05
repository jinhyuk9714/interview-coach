# Multi-stage build for Spring Boot services
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Copy all service source code
COPY gateway gateway
COPY user-service user-service
COPY question-service question-service
COPY interview-service interview-service
COPY feedback-service feedback-service

# Build all services
RUN chmod +x ./gradlew && ./gradlew build -x test --no-daemon

# ============================================
# Runtime images for each service
# ============================================

FROM eclipse-temurin:21-jre-jammy AS user-service
WORKDIR /app
COPY --from=builder /app/user-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

# question-service needs full JRE with native libs for ONNX Runtime (RAG)
FROM eclipse-temurin:21-jre-jammy AS question-service
WORKDIR /app
COPY --from=builder /app/question-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-jammy AS interview-service
WORKDIR /app
COPY --from=builder /app/interview-service/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-jammy AS feedback-service
WORKDIR /app
COPY --from=builder /app/feedback-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-jammy AS gateway
WORKDIR /app
COPY --from=builder /app/gateway/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
