package com.interviewcoach.interview.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddFollowUpRequest {

    @NotNull(message = "부모 QnA ID는 필수입니다")
    private Long parentQnaId;

    @NotBlank(message = "꼬리 질문 내용은 필수입니다")
    private String questionText;

    @NotNull(message = "꼬리 질문 깊이는 필수입니다")
    private Integer followUpDepth;

    private String focusArea;
}
