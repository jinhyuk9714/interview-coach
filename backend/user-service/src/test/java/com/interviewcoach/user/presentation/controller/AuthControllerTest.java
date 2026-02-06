package com.interviewcoach.user.presentation.controller;

import com.interviewcoach.user.application.dto.response.TokenResponse;
import com.interviewcoach.user.application.dto.response.UserResponse;
import com.interviewcoach.user.application.service.AuthService;
import com.interviewcoach.user.exception.DuplicateEmailException;
import com.interviewcoach.user.exception.GlobalExceptionHandler;
import com.interviewcoach.user.exception.InvalidCredentialsException;
import com.interviewcoach.user.exception.InvalidTokenException;
import com.interviewcoach.user.security.jwt.JwtAuthenticationFilter;
import com.interviewcoach.user.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, JwtTokenProvider.class}
        )
)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/v1/auth/signup")
    class SignupTest {

        @Test
        @DisplayName("회원가입 성공 - 201 Created")
        void signup_Success() throws Exception {
            // given
            UserResponse response = UserResponse.builder()
                    .id(1L)
                    .email("test@test.com")
                    .nickname("테스트유저")
                    .build();
            given(authService.signup(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@test.com",
                                        "password": "password123",
                                        "nickname": "테스트유저"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("test@test.com"))
                    .andExpect(jsonPath("$.nickname").value("테스트유저"));
        }

        @Test
        @DisplayName("중복 이메일 회원가입 - 409 Conflict")
        void signup_DuplicateEmail() throws Exception {
            // given
            given(authService.signup(any())).willThrow(new DuplicateEmailException("test@test.com"));

            // when & then
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@test.com",
                                        "password": "password123",
                                        "nickname": "테스트유저"
                                    }
                                    """))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTest {

        @Test
        @DisplayName("로그인 성공 - 200 OK")
        void login_Success() throws Exception {
            // given
            TokenResponse response = TokenResponse.of("access-token", "refresh-token", 3600L);
            given(authService.login(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@test.com",
                                        "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(3600));
        }

        @Test
        @DisplayName("잘못된 자격 증명 로그인 - 401 Unauthorized")
        void login_InvalidCredentials() throws Exception {
            // given
            given(authService.login(any())).willThrow(new InvalidCredentialsException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@test.com",
                                        "password": "wrong-password"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTest {

        @Test
        @DisplayName("토큰 갱신 성공 - 200 OK")
        void refresh_Success() throws Exception {
            // given
            TokenResponse response = TokenResponse.of("new-access", "new-refresh", 3600L);
            given(authService.refresh(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "refreshToken": "valid-refresh-token"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
        }

        @Test
        @DisplayName("유효하지 않은 토큰 갱신 - 401 Unauthorized")
        void refresh_InvalidToken() throws Exception {
            // given
            given(authService.refresh(any())).willThrow(new InvalidTokenException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "refreshToken": "invalid-token"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }
}
