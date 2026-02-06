package com.interviewcoach.user.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.user.application.dto.response.UserResponse;
import com.interviewcoach.user.application.service.UserService;
import com.interviewcoach.user.exception.GlobalExceptionHandler;
import com.interviewcoach.user.exception.UserNotFoundException;
import com.interviewcoach.user.security.jwt.JwtTokenProvider;
import com.interviewcoach.user.security.jwt.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("UserController 단위 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final Long USER_ID = 1L;

    private UsernamePasswordAuthenticationToken createAuthentication(Long userId, String email) {
        UserPrincipal principal = new UserPrincipal(userId, email);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Nested
    @DisplayName("GET /api/v1/users/me - 내 프로필 조회")
    class GetMyProfileTest {

        @Test
        @DisplayName("내 프로필 조회 성공 - 200 OK")
        void getMyProfile_Success() throws Exception {
            // given
            UserResponse response = UserResponse.builder()
                    .id(USER_ID)
                    .email("test@test.com")
                    .nickname("테스트유저")
                    .targetPosition("백엔드 개발자")
                    .experienceYears(3)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(userService.getMyProfile(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/users/me")
                            .with(authentication(createAuthentication(USER_ID, "test@test.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID))
                    .andExpect(jsonPath("$.email").value("test@test.com"))
                    .andExpect(jsonPath("$.nickname").value("테스트유저"))
                    .andExpect(jsonPath("$.targetPosition").value("백엔드 개발자"))
                    .andExpect(jsonPath("$.experienceYears").value(3));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 프로필 조회 - 404 Not Found")
        void getMyProfile_NotFound() throws Exception {
            // given
            given(userService.getMyProfile(999L)).willThrow(new UserNotFoundException(999L));

            // when & then
            mockMvc.perform(get("/api/v1/users/me")
                            .with(authentication(createAuthentication(999L, "unknown@test.com"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me - 프로필 수정")
    class UpdateMyProfileTest {

        @Test
        @DisplayName("프로필 수정 성공 - 200 OK")
        void updateMyProfile_Success() throws Exception {
            // given
            UserResponse response = UserResponse.builder()
                    .id(USER_ID)
                    .email("test@test.com")
                    .nickname("새닉네임")
                    .targetPosition("풀스택 개발자")
                    .experienceYears(5)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(userService.updateMyProfile(eq(USER_ID), any())).willReturn(response);

            String requestBody = """
                {
                    "nickname": "새닉네임",
                    "targetPosition": "풀스택 개발자",
                    "experienceYears": 5
                }
                """;

            // when & then
            mockMvc.perform(put("/api/v1/users/me")
                            .with(authentication(createAuthentication(USER_ID, "test@test.com")))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("새닉네임"))
                    .andExpect(jsonPath("$.targetPosition").value("풀스택 개발자"))
                    .andExpect(jsonPath("$.experienceYears").value(5));
        }

        @Test
        @DisplayName("닉네임만 수정 - 200 OK")
        void updateMyProfile_OnlyNickname() throws Exception {
            // given
            UserResponse response = UserResponse.builder()
                    .id(USER_ID)
                    .email("test@test.com")
                    .nickname("새닉네임")
                    .targetPosition("백엔드 개발자")
                    .experienceYears(3)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(userService.updateMyProfile(eq(USER_ID), any())).willReturn(response);

            String requestBody = """
                {
                    "nickname": "새닉네임"
                }
                """;

            // when & then
            mockMvc.perform(put("/api/v1/users/me")
                            .with(authentication(createAuthentication(USER_ID, "test@test.com")))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("새닉네임"))
                    .andExpect(jsonPath("$.targetPosition").value("백엔드 개발자"));
        }
    }
}
