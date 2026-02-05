package com.interviewcoach.interview.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class StartInterviewRequest {

    @NotNull(message = "JD ID는 필수입니다")
    private Long jdId;

    @NotBlank(message = "면접 유형은 필수입니다")
    private String interviewType; // technical, behavioral, mixed

    private List<QuestionInput> questions;

    @Getter
    @NoArgsConstructor
    public static class QuestionInput {
        private String questionType;
        private String questionText;
    }
}
