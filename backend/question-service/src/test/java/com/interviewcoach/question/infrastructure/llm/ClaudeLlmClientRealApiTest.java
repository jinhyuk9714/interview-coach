package com.interviewcoach.question.infrastructure.llm;

import com.interviewcoach.question.infrastructure.llm.LlmClient.JdAnalysisResult;
import com.interviewcoach.question.infrastructure.llm.LlmClient.GeneratedQuestionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "CLAUDE_API_KEY", matches = ".+")
class ClaudeLlmClientRealApiTest {

    @Autowired
    private ClaudeLlmClient claudeLlmClient;

    @Test
    void analyzeJd_WithRealApi_ReturnsValidResult() {
        // given - 간단한 JD
        String jdContent = "Java 백엔드 개발자 채용. Spring Boot 경험 필수.";

        // when
        JdAnalysisResult result = claudeLlmClient.analyzeJd(jdContent);

        // then
        System.out.println("=== JD 분석 결과 ===");
        System.out.println("Skills: " + result.skills());
        System.out.println("Requirements: " + result.requirements());
        System.out.println("Summary: " + result.summary());

        assertThat(result.skills()).isNotEmpty();
        assertThat(result.summary()).isNotBlank();
    }

    @Test
    void generateQuestions_WithRealApi_ReturnsValidQuestions() {
        // given
        String jdText = "Java 백엔드 개발자";
        List<String> skills = List.of("Java", "Spring Boot");

        // when - 2개 질문만 생성 (비용 절약)
        List<GeneratedQuestionResult> questions = claudeLlmClient.generateQuestions(
                jdText, skills, "technical", 2, 3);

        // then
        System.out.println("=== 생성된 질문 ===");
        for (GeneratedQuestionResult q : questions) {
            System.out.println("- [" + q.questionType() + "] " + q.questionText());
        }

        assertThat(questions).isNotEmpty();
        assertThat(questions.size()).isLessThanOrEqualTo(2);
    }
}
