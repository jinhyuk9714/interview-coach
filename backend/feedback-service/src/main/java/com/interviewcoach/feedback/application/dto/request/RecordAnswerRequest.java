package com.interviewcoach.feedback.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecordAnswerRequest {

    @NotBlank(message = "스킬 카테고리는 필수입니다")
    private String skillCategory;

    @NotNull(message = "정답 여부는 필수입니다")
    private Boolean isCorrect;

    private String weakPoint;
}
