package com.interviewcoach.interview.application.dto.response;

import com.interviewcoach.interview.domain.entity.InterviewQna;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class QnaResponse {

    private Long id;
    private Integer questionOrder;
    private String questionType;
    private String questionText;
    private String answerText;
    private Map<String, Object> feedback;
    private LocalDateTime answeredAt;

    public static QnaResponse from(InterviewQna qna) {
        return QnaResponse.builder()
                .id(qna.getId())
                .questionOrder(qna.getQuestionOrder())
                .questionType(qna.getQuestionType())
                .questionText(qna.getQuestionText())
                .answerText(qna.getAnswerText())
                .feedback(qna.getFeedback())
                .answeredAt(qna.getAnsweredAt())
                .build();
    }
}
