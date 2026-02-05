package com.interviewcoach.user.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    private String password;

    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;

    @Size(max = 100, message = "목표 포지션은 100자 이하여야 합니다")
    private String targetPosition;

    private Integer experienceYears;
}
