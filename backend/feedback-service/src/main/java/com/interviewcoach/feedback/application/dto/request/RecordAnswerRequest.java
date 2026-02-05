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

    // 실제 점수 (0-100). null이면 isCorrect 기반으로 100 또는 0 사용
    private Integer score;
}
