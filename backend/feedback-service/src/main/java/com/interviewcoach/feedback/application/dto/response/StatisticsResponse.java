package com.interviewcoach.feedback.application.dto.response;

import com.interviewcoach.feedback.domain.entity.UserStatistics;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StatisticsResponse {

    private Long userId;
    private String skillCategory;
    private Integer totalQuestions;
    private Integer correctCount;
    private BigDecimal correctRate;
    private List<String> weakPoints;
    private LocalDateTime updatedAt;

    public static StatisticsResponse from(UserStatistics stats) {
        return StatisticsResponse.builder()
                .userId(stats.getUserId())
                .skillCategory(stats.getSkillCategory())
                .totalQuestions(stats.getTotalQuestions())
                .correctCount(stats.getCorrectCount())
                .correctRate(stats.getCorrectRate())
                .weakPoints(stats.getWeakPoints())
                .updatedAt(stats.getUpdatedAt())
                .build();
    }
}
