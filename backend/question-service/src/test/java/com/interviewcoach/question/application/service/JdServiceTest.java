package com.interviewcoach.question.application.service;

import com.interviewcoach.question.application.dto.request.CreateJdRequest;
import com.interviewcoach.question.application.dto.response.JdAnalysisResponse;
import com.interviewcoach.question.application.dto.response.JdResponse;
import com.interviewcoach.question.domain.entity.JobDescription;
import com.interviewcoach.question.domain.repository.JobDescriptionRepository;
import com.interviewcoach.question.exception.JdNotFoundException;
import com.interviewcoach.question.infrastructure.llm.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdService 단위 테스트")
class JdServiceTest {

    @Mock
    private JobDescriptionRepository jdRepository;

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private JdService jdService;

    private static final Long USER_ID = 1L;
    private static final Long JD_ID = 100L;

    @Nested
    @DisplayName("createJd 메서드")
    class CreateJdTest {

        @Test
        @DisplayName("JD 생성 성공")
        void createJd_Success() throws Exception {
            // given
            CreateJdRequest request = createJdRequest("네이버", "백엔드 개발자", "Java, Spring Boot 경험 필수");
            JobDescription savedJd = createJobDescription(JD_ID, USER_ID, "네이버", "백엔드 개발자", "Java, Spring Boot 경험 필수");

            given(jdRepository.save(any(JobDescription.class))).willReturn(savedJd);

            // when
            JdResponse response = jdService.createJd(USER_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(JD_ID);
            assertThat(response.getCompanyName()).isEqualTo("네이버");
            assertThat(response.getPosition()).isEqualTo("백엔드 개발자");
            verify(jdRepository, times(1)).save(any(JobDescription.class));
        }

        @Test
        @DisplayName("회사명 없이 JD 생성 성공")
        void createJd_WithoutCompanyName_Success() throws Exception {
            // given
            CreateJdRequest request = createJdRequest(null, "개발자", "Python 경험자");
            JobDescription savedJd = createJobDescription(JD_ID, USER_ID, null, "개발자", "Python 경험자");

            given(jdRepository.save(any(JobDescription.class))).willReturn(savedJd);

            // when
            JdResponse response = jdService.createJd(USER_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCompanyName()).isNull();
            verify(jdRepository, times(1)).save(any(JobDescription.class));
        }
    }

    @Nested
    @DisplayName("getJdList 메서드")
    class GetJdListTest {

        @Test
        @DisplayName("사용자의 JD 목록 조회 성공")
        void getJdList_Success() throws Exception {
            // given
            List<JobDescription> jdList = List.of(
                    createJobDescription(1L, USER_ID, "카카오", "서버 개발자", "JD 내용 1"),
                    createJobDescription(2L, USER_ID, "라인", "백엔드 개발자", "JD 내용 2")
            );
            given(jdRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).willReturn(jdList);

            // when
            List<JdResponse> responses = jdService.getJdList(USER_ID);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getCompanyName()).isEqualTo("카카오");
            assertThat(responses.get(1).getCompanyName()).isEqualTo("라인");
        }

        @Test
        @DisplayName("JD가 없는 경우 빈 목록 반환")
        void getJdList_Empty() {
            // given
            given(jdRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).willReturn(List.of());

            // when
            List<JdResponse> responses = jdService.getJdList(USER_ID);

            // then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("getJd 메서드")
    class GetJdTest {

        @Test
        @DisplayName("JD 단건 조회 성공")
        void getJd_Success() throws Exception {
            // given
            JobDescription jd = createJobDescription(JD_ID, USER_ID, "토스", "서버 개발자", "JD 내용");
            given(jdRepository.findById(JD_ID)).willReturn(Optional.of(jd));

            // when
            JdResponse response = jdService.getJd(JD_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(JD_ID);
            assertThat(response.getCompanyName()).isEqualTo("토스");
        }

        @Test
        @DisplayName("존재하지 않는 JD 조회 시 예외 발생")
        void getJd_NotFound() {
            // given
            given(jdRepository.findById(JD_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> jdService.getJd(JD_ID))
                    .isInstanceOf(JdNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("analyzeJd 메서드")
    class AnalyzeJdTest {

        @Test
        @DisplayName("JD 분석 성공")
        void analyzeJd_Success() throws Exception {
            // given
            JobDescription jd = createJobDescription(JD_ID, USER_ID, "네이버", "백엔드 개발자",
                    "Java, Spring Boot 기반 백엔드 개발 경험 3년 이상");

            LlmClient.JdAnalysisResult mockResult = new LlmClient.JdAnalysisResult(
                    List.of("Java", "Spring Boot", "JPA"),
                    List.of("3년 이상 경험", "RESTful API 설계"),
                    "백엔드 개발자 포지션입니다."
            );

            given(jdRepository.findById(JD_ID)).willReturn(Optional.of(jd));
            given(llmClient.analyzeJd(anyString())).willReturn(mockResult);

            // when
            JdAnalysisResponse response = jdService.analyzeJd(JD_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getJdId()).isEqualTo(JD_ID);
            assertThat(response.getSkills()).containsExactly("Java", "Spring Boot", "JPA");
            assertThat(response.getRequirements()).containsExactly("3년 이상 경험", "RESTful API 설계");
            assertThat(response.getSummary()).isEqualTo("백엔드 개발자 포지션입니다.");
            verify(llmClient, times(1)).analyzeJd(anyString());
        }

        @Test
        @DisplayName("존재하지 않는 JD 분석 시 예외 발생")
        void analyzeJd_NotFound() {
            // given
            given(jdRepository.findById(JD_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> jdService.analyzeJd(JD_ID))
                    .isInstanceOf(JdNotFoundException.class);
            verify(llmClient, never()).analyzeJd(anyString());
        }
    }

    @Nested
    @DisplayName("deleteJd 메서드")
    class DeleteJdTest {

        @Test
        @DisplayName("JD 삭제 성공")
        void deleteJd_Success() throws Exception {
            // given
            JobDescription jd = createJobDescription(JD_ID, USER_ID, "카카오", "백엔드", "JD 내용");
            given(jdRepository.findById(JD_ID)).willReturn(Optional.of(jd));

            // when
            jdService.deleteJd(USER_ID, JD_ID);

            // then
            verify(jdRepository, times(1)).delete(jd);
        }

        @Test
        @DisplayName("다른 사용자의 JD 삭제 시 예외 발생")
        void deleteJd_Unauthorized() throws Exception {
            // given
            Long otherUserId = 999L;
            JobDescription jd = createJobDescription(JD_ID, USER_ID, "카카오", "백엔드", "JD 내용");
            given(jdRepository.findById(JD_ID)).willReturn(Optional.of(jd));

            // when & then
            assertThatThrownBy(() -> jdService.deleteJd(otherUserId, JD_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 JD를 삭제할 권한이 없습니다.");
            verify(jdRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 JD 삭제 시 예외 발생")
        void deleteJd_NotFound() {
            // given
            given(jdRepository.findById(JD_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> jdService.deleteJd(USER_ID, JD_ID))
                    .isInstanceOf(JdNotFoundException.class);
            verify(jdRepository, never()).delete(any());
        }
    }

    // Helper methods
    private CreateJdRequest createJdRequest(String companyName, String position, String originalText) throws Exception {
        CreateJdRequest request = new CreateJdRequest();
        setField(request, "companyName", companyName);
        setField(request, "position", position);
        setField(request, "originalText", originalText);
        return request;
    }

    private JobDescription createJobDescription(Long id, Long userId, String companyName, String position, String originalText) throws Exception {
        JobDescription jd = JobDescription.builder()
                .userId(userId)
                .companyName(companyName)
                .position(position)
                .originalText(originalText)
                .build();
        setField(jd, "id", id);
        setField(jd, "createdAt", LocalDateTime.now());
        return jd;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
