package com.interviewcoach.user.application.service;

import com.interviewcoach.user.application.dto.request.UpdateProfileRequest;
import com.interviewcoach.user.application.dto.response.UserResponse;
import com.interviewcoach.user.domain.entity.User;
import com.interviewcoach.user.domain.repository.UserRepository;
import com.interviewcoach.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final Long USER_ID = 1L;

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

    private UpdateProfileRequest createUpdateProfileRequest(String nickname, String targetPosition,
                                                              Integer experienceYears) throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        if (nickname != null) {
            Field nicknameField = UpdateProfileRequest.class.getDeclaredField("nickname");
            nicknameField.setAccessible(true);
            nicknameField.set(request, nickname);
        }
        if (targetPosition != null) {
            Field positionField = UpdateProfileRequest.class.getDeclaredField("targetPosition");
            positionField.setAccessible(true);
            positionField.set(request, targetPosition);
        }
        if (experienceYears != null) {
            Field yearsField = UpdateProfileRequest.class.getDeclaredField("experienceYears");
            yearsField.setAccessible(true);
            yearsField.set(request, experienceYears);
        }
        return request;
    }

    @Nested
    @DisplayName("getMyProfile - 프로필 조회")
    class GetMyProfileTest {

        @Test
        @DisplayName("프로필 조회 성공")
        void getMyProfile_Success() throws Exception {
            // given
            User user = createTestUser(USER_ID, "test@test.com");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            UserResponse response = userService.getMyProfile(USER_ID);

            // then
            assertThat(response.getId()).isEqualTo(USER_ID);
            assertThat(response.getEmail()).isEqualTo("test@test.com");
            assertThat(response.getNickname()).isEqualTo("테스트유저");
            assertThat(response.getTargetPosition()).isEqualTo("백엔드 개발자");
            assertThat(response.getExperienceYears()).isEqualTo(3);
            verify(userRepository).findById(USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 프로필 조회 - UserNotFoundException 발생")
        void getMyProfile_NotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getMyProfile(999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateMyProfile - 프로필 수정")
    class UpdateMyProfileTest {

        @Test
        @DisplayName("프로필 수정 성공 - 전체 필드 업데이트")
        void updateMyProfile_Success_AllFields() throws Exception {
            // given
            User user = createTestUser(USER_ID, "test@test.com");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            UpdateProfileRequest request = createUpdateProfileRequest(
                    "새닉네임", "풀스택 개발자", 5);

            // when
            UserResponse response = userService.updateMyProfile(USER_ID, request);

            // then
            assertThat(response.getId()).isEqualTo(USER_ID);
            assertThat(response.getNickname()).isEqualTo("새닉네임");
            assertThat(response.getTargetPosition()).isEqualTo("풀스택 개발자");
            assertThat(response.getExperienceYears()).isEqualTo(5);
            verify(userRepository).findById(USER_ID);
        }

        @Test
        @DisplayName("프로필 수정 - 닉네임만 업데이트")
        void updateMyProfile_OnlyNickname() throws Exception {
            // given
            User user = createTestUser(USER_ID, "test@test.com");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            UpdateProfileRequest request = createUpdateProfileRequest("새닉네임", null, null);

            // when
            UserResponse response = userService.updateMyProfile(USER_ID, request);

            // then
            assertThat(response.getNickname()).isEqualTo("새닉네임");
            assertThat(response.getTargetPosition()).isEqualTo("백엔드 개발자"); // 기존 값 유지
            assertThat(response.getExperienceYears()).isEqualTo(3); // 기존 값 유지
        }

        @Test
        @DisplayName("존재하지 않는 사용자 프로필 수정 - UserNotFoundException 발생")
        void updateMyProfile_NotFound() throws Exception {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());
            UpdateProfileRequest request = createUpdateProfileRequest("새닉네임", null, null);

            // when & then
            assertThatThrownBy(() -> userService.updateMyProfile(999L, request))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
