package com.interviewcoach.feedback.application.service;

import com.interviewcoach.feedback.application.dto.request.RecordAnswerRequest;
import com.interviewcoach.feedback.application.dto.response.StatisticsResponse;
import com.interviewcoach.feedback.application.dto.response.UserStatisticsSummaryResponse;
import com.interviewcoach.feedback.application.dto.response.UserStatisticsSummaryResponse.*;
import com.interviewcoach.feedback.domain.entity.DailyActivity;
import com.interviewcoach.feedback.domain.entity.UserStatistics;
import com.interviewcoach.feedback.domain.repository.DailyActivityRepository;
import com.interviewcoach.feedback.domain.repository.UserStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserStatisticsRepository statisticsRepository;
    private final DailyActivityRepository dailyActivityRepository;

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
        // Use actual score if provided, otherwise default based on isCorrect
        int score = request.getScore() != null ? request.getScore()
                : (request.getIsCorrect() ? 100 : 0);
        stats.recordAnswer(request.getIsCorrect(), score);

        if (request.getWeakPoint() != null && !request.getWeakPoint().isBlank()) {
            stats.addWeakPoint(request.getWeakPoint());
        }

        // Record daily activity
        LocalDate today = LocalDate.now(KST);
        DailyActivity dailyActivity = dailyActivityRepository
                .findByUserIdAndActivityDate(userId, today)
                .orElseGet(() -> {
                    DailyActivity newActivity = DailyActivity.builder()
                            .userId(userId)
                            .activityDate(today)
                            .build();
                    return dailyActivityRepository.save(newActivity);
                });
        dailyActivity.recordActivity(score);

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

        // Calculate actual average score from totalScore across all categories
        int totalScore = statsList.stream()
                .mapToInt(UserStatistics::getTotalScore)
                .sum();

        // overallRate now represents actual average score, not percentage of correct answers
        BigDecimal overallRate = totalQuestions > 0
                ? BigDecimal.valueOf(totalScore)
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

        // Calculate extended statistics - avgScore is now actual average, not percentage
        int avgScore = overallRate.intValue();
        int totalInterviews = statsList.isEmpty() ? 0 : Math.max(1, totalQuestions / 5);
        String rank = calculateRank(avgScore);

        // Category stats with trends
        List<CategoryStatResponse> categoryStats = statsList.stream()
                .map(s -> CategoryStatResponse.builder()
                        .category(s.getSkillCategory())
                        .score(s.getCorrectRate().intValue())
                        .total(s.getTotalQuestions())
                        .trend(0) // Simplified: no historical data yet
                        .build())
                .collect(Collectors.toList());

        // Weekly activity from actual DB records
        List<WeeklyActivityResponse> weeklyActivity = generateWeeklyActivity(userId);

        // Recent progress (generate sample data based on overall score)
        List<ProgressPointResponse> recentProgress = generateRecentProgress(avgScore, totalInterviews);

        // Weak points with suggestions
        List<WeakPointResponse> weakPoints = statsList.stream()
                .filter(s -> s.getCorrectRate().compareTo(BigDecimal.valueOf(70)) < 0)
                .sorted(Comparator.comparing(UserStatistics::getCorrectRate))
                .limit(3)
                .map(s -> WeakPointResponse.builder()
                        .skill(s.getSkillCategory())
                        .score(s.getCorrectRate().intValue())
                        .suggestion(generateSuggestion(s.getSkillCategory()))
                        .build())
                .collect(Collectors.toList());

        return UserStatisticsSummaryResponse.builder()
                .userId(userId)
                .totalQuestions(totalQuestions)
                .totalCorrect(totalCorrect)
                .overallCorrectRate(overallRate)
                .byCategory(byCategory)
                .topWeakPoints(topWeakPoints)
                .totalInterviews(totalInterviews)
                .avgScore(avgScore)
                .scoreTrend(totalInterviews > 1 ? 3 : 0)
                .streakDays(totalInterviews > 0 ? Math.min(totalInterviews, 7) : 0)
                .rank(rank)
                .categoryStats(categoryStats)
                .weeklyActivity(weeklyActivity)
                .recentProgress(recentProgress)
                .weakPoints(weakPoints)
                .build();
    }

    private String calculateRank(int score) {
        if (score >= 90) return "S";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 60) return "C";
        if (score >= 50) return "D";
        return "-";
    }

    private List<WeeklyActivityResponse> generateWeeklyActivity(Long userId) {
        List<WeeklyActivityResponse> activity = new ArrayList<>();
        LocalDate today = LocalDate.now(KST);
        LocalDate weekAgo = today.minusDays(6);

        // Fetch actual activity data from DB
        List<DailyActivity> dailyActivities = dailyActivityRepository
                .findByUserIdAndActivityDateBetween(userId, weekAgo, today);

        // Create a map for quick lookup
        Map<LocalDate, DailyActivity> activityMap = dailyActivities.stream()
                .collect(Collectors.toMap(DailyActivity::getActivityDate, a -> a));

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);

            DailyActivity dayActivity = activityMap.get(date);
            int count = dayActivity != null ? dayActivity.getQuestionCount() : 0;
            int score = dayActivity != null ? dayActivity.getAverageScore() : 0;

            activity.add(WeeklyActivityResponse.builder()
                    .day(dayName)
                    .count(count)
                    .score(score)
                    .build());
        }
        return activity;
    }

    private List<ProgressPointResponse> generateRecentProgress(int avgScore, int totalInterviews) {
        List<ProgressPointResponse> progress = new ArrayList<>();

        if (totalInterviews == 0) {
            return progress;
        }

        // Only show today's data point with actual average score - no random values
        LocalDate today = LocalDate.now(KST);
        progress.add(ProgressPointResponse.builder()
                .date(String.format("%d/%d", today.getMonthValue(), today.getDayOfMonth()))
                .score(avgScore)
                .build());

        return progress;
    }

    private String generateSuggestion(String skillCategory) {
        Map<String, String> suggestions = Map.of(
                "Java", "기본 문법과 OOP 개념을 복습하세요",
                "Spring", "Spring Core와 DI 패턴을 학습하세요",
                "Database", "SQL 쿼리 최적화를 연습하세요",
                "Algorithm", "자료구조 기초부터 복습하세요",
                "System Design", "대용량 시스템 설계 패턴을 학습하세요",
                "behavioral", "STAR 기법으로 답변을 구조화하세요",
                "technical", "핵심 개념을 예제와 함께 정리하세요"
        );
        return suggestions.getOrDefault(skillCategory, "관련 개념을 복습하고 실전 문제를 풀어보세요");
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
