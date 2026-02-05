package com.interviewcoach.question.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GeneratedQuestionsResponse {

    private Long jdId;
    private int totalCount;
    private List<QuestionResponse> questions;
}
