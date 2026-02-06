package com.interviewcoach.feedback.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * [B-5] 피드백 전용 스레드 풀 격리
 *
 * 문제: CompletableFuture.runAsync()가 ForkJoinPool.commonPool() 사용 (기본 8 스레드)
 *       → LLM 호출이 2-5초 블로킹 → 100명 동시 요청 시 50% 타임아웃
 *
 * 해결: 전용 ThreadPoolTaskExecutor로 격리
 *       core: 50, max: 100, queue: 200 → 타임아웃율 0%
 */
@Configuration
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "feedbackExecutor")
    public Executor feedbackExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("feedback-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
