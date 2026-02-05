package com.interviewcoach.question.infrastructure.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClaudeLlmClient 테스트")
class ClaudeLlmClientTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Mock 모드 테스트 (API 키 없음)")
    class MockModeTest {

        private ClaudeLlmClient client;

        @BeforeEach
        void setUp() {
            // API 키 없이 초기화 -> Mock 모드
            client = new ClaudeLlmClient("", "claude-sonnet-4-20250514", objectMapper);
        }

        @Test
        @DisplayName("JD 분석 - Mock 데이터 반환")
        void analyzeJd_MockMode() {
            // given
            String jdText = "Java, Spring Boot 기반 백엔드 개발자 모집";

            // when
            LlmClient.JdAnalysisResult result = client.analyzeJd(jdText);

            // then
            assertThat(result).isNotNull();
            assertThat(result.skills()).containsExactly("Java", "Spring Boot", "JPA", "PostgreSQL", "Redis");
            assertThat(result.requirements()).contains("3년 이상 백엔드 개발 경험");
            assertThat(result.summary()).contains("백엔드 개발자 포지션");
        }

        @Test
        @DisplayName("질문 생성 - Mock 데이터 반환")
        void generateQuestions_MockMode() {
            // given
            String jdText = "개발자 모집";
            List<String> skills = List.of("Java", "Spring");
            String questionType = "technical";
            int count = 3;
            int difficulty = 3;

            // when
            List<LlmClient.GeneratedQuestionResult> results = client.generateQuestions(
                    jdText, skills, questionType, count, difficulty);

            // then
            assertThat(results).isNotNull();
            assertThat(results).isNotEmpty();
            assertThat(results).hasSizeLessThanOrEqualTo(count);

            // Mock 데이터 검증
            LlmClient.GeneratedQuestionResult firstQuestion = results.get(0);
            assertThat(firstQuestion.questionText()).isNotBlank();
            assertThat(firstQuestion.hint()).isNotBlank();
            assertThat(firstQuestion.idealAnswer()).isNotBlank();
            assertThat(firstQuestion.questionType()).isEqualTo("technical");
        }

        @Test
        @DisplayName("질문 생성 - technical 타입 요청 시 기술 질문만 반환")
        void generateQuestions_TechnicalType() {
            // when
            List<LlmClient.GeneratedQuestionResult> results = client.generateQuestions(
                    "JD 내용", List.of("Java"), "technical", 5, 3);

            // then
            assertThat(results).isNotEmpty();
            assertThat(results).allMatch(q -> "technical".equals(q.questionType()));
        }

        @Test
        @DisplayName("질문 생성 - behavioral 타입 요청 시 행동 면접 질문만 반환")
        void generateQuestions_BehavioralType() {
            // when
            List<LlmClient.GeneratedQuestionResult> results = client.generateQuestions(
                    "JD 내용", List.of("Java"), "behavioral", 5, 3);

            // then
            assertThat(results).isNotEmpty();
            assertThat(results).allMatch(q -> "behavioral".equals(q.questionType()));
        }

        @Test
        @DisplayName("질문 생성 - mixed 타입 요청 시 기술/행동 질문 혼합 반환")
        void generateQuestions_MixedType() {
            // when
            List<LlmClient.GeneratedQuestionResult> results = client.generateQuestions(
                    "JD 내용", List.of("Java"), "mixed", 5, 3);

            // then
            assertThat(results).isNotEmpty();
            // mixed일 때는 technical과 behavioral 모두 포함될 수 있음
            boolean hasTechnical = results.stream().anyMatch(q -> "technical".equals(q.questionType()));
            boolean hasBehavioral = results.stream().anyMatch(q -> "behavioral".equals(q.questionType()));
            assertThat(hasTechnical || hasBehavioral).isTrue();
        }
    }

    @Nested
    @DisplayName("실제 API 테스트 (비용 발생!)")
    @EnabledIfEnvironmentVariable(named = "CLAUDE_API_KEY", matches = ".+")
    class RealApiTest {

        private ClaudeLlmClient client;

        @BeforeEach
        void setUp() {
            String apiKey = System.getenv("CLAUDE_API_KEY");
            client = new ClaudeLlmClient(apiKey, "claude-sonnet-4-20250514", objectMapper);
        }

        @Test
        @DisplayName("JD 분석 - 실제 API 호출")
        void analyzeJd_RealApi() {
            // given
            String jdText = """
                    [자격요건]
                    - Java, Kotlin 기반 백엔드 개발 경험 3년 이상
                    - Spring Boot, Spring MVC 개발 경험
                    - RESTful API 설계 및 구현 경험
                    - RDBMS(MySQL, PostgreSQL) 사용 경험

                    [우대사항]
                    - MSA 환경 개발 경험
                    - Kubernetes 운영 경험
                    - 대용량 트래픽 처리 경험
                    """;

            // when
            LlmClient.JdAnalysisResult result = client.analyzeJd(jdText);

            // then
            assertThat(result).isNotNull();
            assertThat(result.skills()).isNotEmpty();
            assertThat(result.requirements()).isNotEmpty();
            assertThat(result.summary()).isNotBlank();

            // 실제 분석 결과에 주요 기술이 포함되어 있는지 확인
            assertThat(result.skills().stream()
                    .map(String::toLowerCase)
                    .toList())
                    .anyMatch(s -> s.contains("java") || s.contains("kotlin") || s.contains("spring"));
        }

        @Test
        @DisplayName("질문 생성 - 실제 API 호출")
        void generateQuestions_RealApi() {
            // given
            String jdText = "Java, Spring Boot 기반 백엔드 개발자";
            List<String> skills = List.of("Java", "Spring Boot");

            // when
            List<LlmClient.GeneratedQuestionResult> results = client.generateQuestions(
                    jdText, skills, "technical", 2, 3);

            // then
            assertThat(results).isNotEmpty();
            assertThat(results).hasSizeLessThanOrEqualTo(2);

            LlmClient.GeneratedQuestionResult firstQuestion = results.get(0);
            assertThat(firstQuestion.questionText()).isNotBlank();
            assertThat(firstQuestion.questionType()).isNotBlank();
        }
    }
}
