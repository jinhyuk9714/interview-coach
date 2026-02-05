package com.interviewcoach.question.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 유사 질문 검색 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarQuestionsResponse {

    private String query;
    private Long jdId;
    private int totalCount;
    private List<SimilarQuestionDto> questions;
    private boolean ragEnabled;
}
