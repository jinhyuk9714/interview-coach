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
}
