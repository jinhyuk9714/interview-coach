package com.interviewcoach.interview.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubmitAnswerRequest {

    @NotNull(message = "질문 순서는 필수입니다")
    private Integer questionOrder;

    @NotBlank(message = "답변 내용은 필수입니다")
    private String answerText;
}
