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
# [B-7] JVM 힙/GC 튜닝 적용
#
# Before: java -jar app.jar (JVM 기본값, 과소 할당, 빈번한 Full GC)
# Phase 1: G1GC + 힙 사이징
# Phase 2: ZGC 전환 → Max GC Pause 5ms, P99 300ms
# ============================================

# 공통 JVM 옵션
# -Xms: 초기 힙 (예약으로 GC 빈도 감소)
# -Xmx: 최대 힙
# -XX:+UseZGC -XX:+ZGenerational: 저지연 GC (JDK 21+)
# -Xlog:gc*: GC 로그 활성화 (GCEasy 분석용)

FROM eclipse-temurin:21-jre-jammy AS user-service
WORKDIR /app
COPY --from=builder /app/user-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", \
  "-Xms128m", "-Xmx256m", \
  "-XX:+UseZGC", "-XX:+ZGenerational", \
  "-Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags:filecount=5,filesize=10m", \
  "-jar", "app.jar"]

# question-service needs full JRE with native libs for ONNX Runtime (RAG)
FROM eclipse-temurin:21-jre-jammy AS question-service
WORKDIR /app
COPY --from=builder /app/question-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", \
  "-Xms256m", "-Xmx512m", \
  "-XX:+UseZGC", "-XX:+ZGenerational", \
  "-Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags:filecount=5,filesize=10m", \
  "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-jammy AS interview-service
WORKDIR /app
COPY --from=builder /app/interview-service/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", \
  "-Xms128m", "-Xmx256m", \
  "-XX:+UseZGC", "-XX:+ZGenerational", \
  "-Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags:filecount=5,filesize=10m", \
  "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-jammy AS feedback-service
WORKDIR /app
COPY --from=builder /app/feedback-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", \
  "-Xms256m", "-Xmx512m", \
  "-XX:+UseZGC", "-XX:+ZGenerational", \
  "-Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags:filecount=5,filesize=10m", \
  "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-jammy AS gateway
WORKDIR /app
COPY --from=builder /app/gateway/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-Xms128m", "-Xmx256m", \
  "-XX:+UseZGC", "-XX:+ZGenerational", \
  "-Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags:filecount=5,filesize=10m", \
  "-jar", "app.jar"]
