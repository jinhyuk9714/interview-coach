package com.interviewcoach.user.presentation.controller;

import com.interviewcoach.user.application.dto.request.UpdateProfileRequest;
import com.interviewcoach.user.application.dto.response.UserResponse;
import com.interviewcoach.user.application.service.UserService;
import com.interviewcoach.user.security.jwt.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 프로필 관리")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필 정보 조회")
    @ApiResponse(responseCode = "200", description = "프로필 조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = userService.getMyProfile(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 프로필 수정", description = "로그인한 사용자의 프로필 정보 수정")
    @ApiResponse(responseCode = "200", description = "프로필 수정 성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateMyProfile(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
