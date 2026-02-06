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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User createTestUser(Long id, String email) throws Exception {
        User user = User.builder()
                .email(email)
                .password("encoded-password")
                .nickname("테스트유저")
                .targetPosition("백엔드 개발자")
                .experienceYears(3)
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }

    private SignupRequest createSignupRequest() throws Exception {
        SignupRequest request = new SignupRequest();
        Field emailField = SignupRequest.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(request, "test@test.com");
        Field passwordField = SignupRequest.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        passwordField.set(request, "password123");
        Field nicknameField = SignupRequest.class.getDeclaredField("nickname");
        nicknameField.setAccessible(true);
        nicknameField.set(request, "테스트유저");
        return request;
    }

    private LoginRequest createLoginRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        Field emailField = LoginRequest.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(request, "test@test.com");
        Field passwordField = LoginRequest.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        passwordField.set(request, "password123");
        return request;
    }

    private RefreshTokenRequest createRefreshTokenRequest(String token) throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        Field tokenField = RefreshTokenRequest.class.getDeclaredField("refreshToken");
        tokenField.setAccessible(true);
        tokenField.set(request, token);
        return request;
    }

    @Nested
    @DisplayName("회원가입")
    class SignupTest {

        @Test
        @DisplayName("회원가입 성공")
        void signup_Success() throws Exception {
            // given
            SignupRequest request = createSignupRequest();
            User savedUser = createTestUser(1L, "test@test.com");

            given(userRepository.existsByEmail("test@test.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encoded-password");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            UserResponse response = authService.signup(request);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("test@test.com");
            assertThat(response.getNickname()).isEqualTo("테스트유저");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("중복 이메일로 회원가입 실패")
        void signup_DuplicateEmail_ThrowsException() throws Exception {
            // given
            SignupRequest request = createSignupRequest();
            given(userRepository.existsByEmail("test@test.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(DuplicateEmailException.class);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("로그인")
    class LoginTest {

        @Test
        @DisplayName("로그인 성공")
        void login_Success() throws Exception {
            // given
            LoginRequest request = createLoginRequest();
            User user = createTestUser(1L, "test@test.com");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
            given(jwtTokenProvider.createAccessToken(1L, "test@test.com")).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(1L, "test@test.com")).willReturn("refresh-token");
            given(jwtTokenProvider.getAccessTokenExpiration()).willReturn(3600000L);

            // when
            TokenResponse response = authService.login(request);

            // then
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 실패")
        void login_EmailNotFound_ThrowsException() throws Exception {
            // given
            LoginRequest request = createLoginRequest();
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 실패")
        void login_WrongPassword_ThrowsException() throws Exception {
            // given
            LoginRequest request = createLoginRequest();
            User user = createTestUser(1L, "test@test.com");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password123", "encoded-password")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class RefreshTest {

        @Test
        @DisplayName("토큰 갱신 성공")
        void refresh_Success() throws Exception {
            // given
            RefreshTokenRequest request = createRefreshTokenRequest("valid-refresh-token");

            given(jwtTokenProvider.validateToken("valid-refresh-token")).willReturn(true);
            given(jwtTokenProvider.isRefreshToken("valid-refresh-token")).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken("valid-refresh-token")).willReturn(1L);
            given(jwtTokenProvider.getEmailFromToken("valid-refresh-token")).willReturn("test@test.com");
            given(userRepository.existsById(1L)).willReturn(true);
            given(jwtTokenProvider.createAccessToken(1L, "test@test.com")).willReturn("new-access-token");
            given(jwtTokenProvider.createRefreshToken(1L, "test@test.com")).willReturn("new-refresh-token");
            given(jwtTokenProvider.getAccessTokenExpiration()).willReturn(3600000L);

            // when
            TokenResponse response = authService.refresh(request);

            // then
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 갱신 실패")
        void refresh_InvalidToken_ThrowsException() throws Exception {
            // given
            RefreshTokenRequest request = createRefreshTokenRequest("invalid-token");
            given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Access 토큰으로 갱신 시도 실패")
        void refresh_NotRefreshToken_ThrowsException() throws Exception {
            // given
            RefreshTokenRequest request = createRefreshTokenRequest("access-token");
            given(jwtTokenProvider.validateToken("access-token")).willReturn(true);
            given(jwtTokenProvider.isRefreshToken("access-token")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("삭제된 사용자의 토큰으로 갱신 실패")
        void refresh_UserNotFound_ThrowsException() throws Exception {
            // given
            RefreshTokenRequest request = createRefreshTokenRequest("valid-refresh-token");

            given(jwtTokenProvider.validateToken("valid-refresh-token")).willReturn(true);
            given(jwtTokenProvider.isRefreshToken("valid-refresh-token")).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken("valid-refresh-token")).willReturn(999L);
            given(jwtTokenProvider.getEmailFromToken("valid-refresh-token")).willReturn("deleted@test.com");
            given(userRepository.existsById(999L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
