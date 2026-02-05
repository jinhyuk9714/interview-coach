package com.interviewcoach.question.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유사 질문 개별 항목 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarQuestionDto {

    private Long questionId;
    private Long jdId;
    private String questionType;
    private String skillCategory;
    private String content;
    private double similarityScore;
}
