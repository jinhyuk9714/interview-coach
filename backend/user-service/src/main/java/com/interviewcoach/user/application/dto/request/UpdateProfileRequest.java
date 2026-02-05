package com.interviewcoach.user.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;

    @Size(max = 100, message = "목표 포지션은 100자 이하여야 합니다")
    private String targetPosition;

    private Integer experienceYears;
}
