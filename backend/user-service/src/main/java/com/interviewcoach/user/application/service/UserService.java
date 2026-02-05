package com.interviewcoach.user.application.service;

import com.interviewcoach.user.application.dto.request.UpdateProfileRequest;
import com.interviewcoach.user.application.dto.response.UserResponse;
import com.interviewcoach.user.domain.entity.User;
import com.interviewcoach.user.domain.repository.UserRepository;
import com.interviewcoach.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.updateProfile(
                request.getNickname(),
                request.getTargetPosition(),
                request.getExperienceYears()
        );

        return UserResponse.from(user);
    }
}
