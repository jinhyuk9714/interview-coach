package com.interviewcoach.user.presentation.controller;

import com.interviewcoach.user.application.dto.request.UpdateProfileRequest;
import com.interviewcoach.user.application.dto.response.UserResponse;
import com.interviewcoach.user.application.service.UserService;
import com.interviewcoach.user.security.jwt.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = userService.getMyProfile(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateMyProfile(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
