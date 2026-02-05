package com.interviewcoach.feedback.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FeedbackResponse {

    private Long sessionId;
    private Long qnaId;
    private Integer score;
    private List<String> strengths;
    private List<String> improvements;
    private String tips;
    private String overallComment;
}
