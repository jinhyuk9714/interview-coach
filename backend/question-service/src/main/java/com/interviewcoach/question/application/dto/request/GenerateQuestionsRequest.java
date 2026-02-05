package com.interviewcoach.question.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GenerateQuestionsRequest {

    @NotNull(message = "JD ID는 필수입니다")
    private Long jdId;

    private String questionType; // technical, behavioral, mixed

    @Min(value = 1, message = "최소 1개 이상의 질문을 생성해야 합니다")
    @Max(value = 20, message = "최대 20개까지 질문을 생성할 수 있습니다")
    private Integer count = 5;

    @Min(value = 1, message = "난이도는 1 이상이어야 합니다")
    @Max(value = 5, message = "난이도는 5 이하여야 합니다")
    private Integer difficulty = 3;
}
