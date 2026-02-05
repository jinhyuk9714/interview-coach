package com.interviewcoach.user.application.dto.response;

import com.interviewcoach.user.domain.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String nickname;
    private String targetPosition;
    private Integer experienceYears;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .targetPosition(user.getTargetPosition())
                .experienceYears(user.getExperienceYears())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
