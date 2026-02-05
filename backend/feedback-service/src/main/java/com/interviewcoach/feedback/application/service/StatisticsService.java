package com.interviewcoach.feedback.application.service;

import com.interviewcoach.feedback.application.dto.request.RecordAnswerRequest;
import com.interviewcoach.feedback.application.dto.response.StatisticsResponse;
import com.interviewcoach.feedback.application.dto.response.UserStatisticsSummaryResponse;
import com.interviewcoach.feedback.domain.entity.UserStatistics;
import com.interviewcoach.feedback.domain.repository.UserStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserStatisticsRepository statisticsRepository;

    /**
     * [동시성 이슈 포인트] - 락 없이 통계 업데이트
     * 여러 스레드가 동시에 같은 사용자의 같은 카테고리 통계를 업데이트하면
     * Lost Update가 발생할 수 있음
     *
     * 4주차 최적화 예정:
     * 1. @Lock(LockModeType.PESSIMISTIC_WRITE) 사용
     * 2. 또는 분산 락 (Redis) 사용
     */
    @Transactional
    public StatisticsResponse recordAnswer(Long userId, RecordAnswerRequest request) {
        // 락 없이 조회 - race condition 가능!
        UserStatistics stats = statisticsRepository
                .findByUserIdAndSkillCategory(userId, request.getSkillCategory())
                .orElseGet(() -> {
                    UserStatistics newStats = UserStatistics.builder()
                            .userId(userId)
                            .skillCategory(request.getSkillCategory())
                            .build();
                    return statisticsRepository.save(newStats);
                });

        // 동시성 이슈 발생 지점: 읽기 -> 수정 -> 쓰기 비원자적
        stats.recordAnswer(request.getIsCorrect());

        if (request.getWeakPoint() != null && !request.getWeakPoint().isBlank()) {
            stats.addWeakPoint(request.getWeakPoint());
        }

        log.info("Recorded answer: userId={}, category={}, isCorrect={}, total={}",
                userId, request.getSkillCategory(), request.getIsCorrect(), stats.getTotalQuestions());

        return StatisticsResponse.from(stats);
    }

    @Transactional(readOnly = true)
    public UserStatisticsSummaryResponse getStatistics(Long userId) {
        List<UserStatistics> statsList = statisticsRepository.findByUserIdOrderByCorrectRateDesc(userId);

        int totalQuestions = statsList.stream()
                .mapToInt(UserStatistics::getTotalQuestions)
                .sum();

        int totalCorrect = statsList.stream()
                .mapToInt(UserStatistics::getCorrectCount)
                .sum();

        BigDecimal overallRate = totalQuestions > 0
                ? BigDecimal.valueOf(totalCorrect)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalQuestions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<String> topWeakPoints = statsList.stream()
                .flatMap(s -> s.getWeakPoints().stream())
                .distinct()
                .limit(5)
                .toList();

        List<StatisticsResponse> byCategory = statsList.stream()
                .map(StatisticsResponse::from)
                .collect(Collectors.toList());

        return UserStatisticsSummaryResponse.builder()
                .userId(userId)
                .totalQuestions(totalQuestions)
                .totalCorrect(totalCorrect)
                .overallCorrectRate(overallRate)
                .byCategory(byCategory)
                .topWeakPoints(topWeakPoints)
                .build();
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatisticsByCategory(Long userId, String skillCategory) {
        UserStatistics stats = statisticsRepository
                .findByUserIdAndSkillCategory(userId, skillCategory)
                .orElse(UserStatistics.builder()
                        .userId(userId)
                        .skillCategory(skillCategory)
                        .build());

        return StatisticsResponse.from(stats);
    }
}
