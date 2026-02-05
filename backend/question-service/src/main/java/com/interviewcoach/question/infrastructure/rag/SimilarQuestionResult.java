package com.interviewcoach.question.infrastructure.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ChromaDB에서 검색된 유사 질문 결과를 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarQuestionResult {

    private Long questionId;
    private Long jdId;
    private String questionType;
    private String skillCategory;
    private String content;
    private double score;

    /**
     * 프롬프트에 포함할 문자열 형태로 변환
     */
    public String toPromptString() {
        return String.format("[%s/%s] %s", questionType, skillCategory, content);
    }
}
