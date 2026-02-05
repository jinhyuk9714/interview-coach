package com.interviewcoach.user.application.service;

import com.interviewcoach.user.application.dto.request.LoginRequest;
import com.interviewcoach.user.application.dto.request.RefreshTokenRequest;
import com.interviewcoach.user.application.dto.request.SignupRequest;
import com.interviewcoach.user.application.dto.response.TokenResponse;
import com.interviewcoach.user.application.dto.response.UserResponse;
import com.interviewcoach.user.domain.entity.User;
import com.interviewcoach.user.domain.repository.UserRepository;
import com.interviewcoach.user.exception.DuplicateEmailException;
import com.interviewcoach.user.exception.InvalidCredentialsException;
import com.interviewcoach.user.exception.InvalidTokenException;
import com.interviewcoach.user.exception.UserNotFoundException;
import com.interviewcoach.user.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .targetPosition(request.getTargetPosition())
                .experienceYears(request.getExperienceYears())
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        return TokenResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration() / 1000
        );
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken) ||
            !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // Verify user still exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId, email);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, email);

        return TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpiration() / 1000
        );
    }
}
