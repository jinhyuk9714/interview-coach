package com.interviewcoach.interview.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubmitAnswerRequest {

    @NotNull(message = "질문 순서는 필수입니다")
    private Integer questionOrder;

    @NotBlank(message = "답변 내용은 필수입니다")
    @Size(max = 5000, message = "답변은 5000자 이내여야 합니다")
    private String answerText;
}
