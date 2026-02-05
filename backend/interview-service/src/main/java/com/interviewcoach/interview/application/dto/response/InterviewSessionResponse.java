package com.interviewcoach.interview.application.dto.response;

import com.interviewcoach.interview.domain.entity.InterviewSession;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InterviewSessionResponse {

    private Long id;
    private Long userId;
    private Long jdId;
    private String interviewType;
    private String status;
    private Integer totalQuestions;
    private BigDecimal avgScore;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<QnaResponse> qnaList;

    public static InterviewSessionResponse from(InterviewSession session) {
        return InterviewSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .jdId(session.getJdId())
                .interviewType(session.getInterviewType())
                .status(session.getStatus())
                .totalQuestions(session.getTotalQuestions())
                .avgScore(session.getAvgScore())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .build();
    }

    public static InterviewSessionResponse fromWithQna(InterviewSession session) {
        return InterviewSessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .jdId(session.getJdId())
                .interviewType(session.getInterviewType())
                .status(session.getStatus())
                .totalQuestions(session.getTotalQuestions())
                .avgScore(session.getAvgScore())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .qnaList(session.getQnaList().stream()
                        .map(QnaResponse::from)
                        .toList())
                .build();
    }
}
