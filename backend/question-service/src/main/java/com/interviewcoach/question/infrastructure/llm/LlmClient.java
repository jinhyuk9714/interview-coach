package com.interviewcoach.question.infrastructure.llm;

import java.util.List;

public interface LlmClient {

    JdAnalysisResult analyzeJd(String jdText);

    List<GeneratedQuestionResult> generateQuestions(String jdText, List<String> skills,
                                                     String questionType, int count, int difficulty);

    record JdAnalysisResult(
            List<String> skills,
            List<String> requirements,
            String summary
    ) {}

    record GeneratedQuestionResult(
            String questionType,
            String skillCategory,
            String questionText,
            String hint,
            String idealAnswer,
            int difficulty
    ) {}
}
