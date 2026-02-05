package com.interviewcoach.feedback.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class UserStatisticsSummaryResponse {

    private Long userId;
    private Integer totalQuestions;
    private Integer totalCorrect;
    private BigDecimal overallCorrectRate;
    private List<StatisticsResponse> byCategory;
    private List<String> topWeakPoints;

    // Extended statistics
    private Integer totalInterviews;
    private Integer avgScore;
    private Integer scoreTrend;
    private Integer streakDays;
    private String rank;
    private List<CategoryStatResponse> categoryStats;
    private List<WeeklyActivityResponse> weeklyActivity;
    private List<ProgressPointResponse> recentProgress;
    private List<WeakPointResponse> weakPoints;

    @Getter
    @Builder
    public static class CategoryStatResponse {
        private String category;
        private Integer score;
        private Integer total;
        private Integer trend;
    }

    @Getter
    @Builder
    public static class WeeklyActivityResponse {
        private String day;
        private Integer count;
        private Integer score;
    }

    @Getter
    @Builder
    public static class ProgressPointResponse {
        private String date;
        private Integer score;
    }

    @Getter
    @Builder
    public static class WeakPointResponse {
        private String skill;
        private Integer score;
        private String suggestion;
    }
}
