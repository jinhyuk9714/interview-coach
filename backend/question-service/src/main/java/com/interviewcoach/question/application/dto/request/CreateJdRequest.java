package com.interviewcoach.question.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateJdRequest {

    @Size(max = 100, message = "회사명은 100자 이하여야 합니다")
    private String companyName;

    @Size(max = 100, message = "포지션은 100자 이하여야 합니다")
    private String position;

    @NotBlank(message = "JD 내용은 필수입니다")
    @Size(min = 50, max = 10000, message = "JD 내용은 50자 이상 10000자 이내여야 합니다")
    private String originalText;

    @Size(max = 500, message = "URL은 500자 이하여야 합니다")
    private String originalUrl;
}
