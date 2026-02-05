package com.interviewcoach.question.application.dto.response;

import com.interviewcoach.question.domain.entity.GeneratedQuestion;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionResponse {

    private Long id;
    private Long jdId;
    private String questionType;
    private String skillCategory;
    private String questionText;
    private String hint;
    private Integer difficulty;

    public static QuestionResponse from(GeneratedQuestion question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .jdId(question.getJdId())
                .questionType(question.getQuestionType())
                .skillCategory(question.getSkillCategory())
                .questionText(question.getQuestionText())
                .hint(question.getHint())
                .difficulty(question.getDifficulty())
                .build();
    }
}
