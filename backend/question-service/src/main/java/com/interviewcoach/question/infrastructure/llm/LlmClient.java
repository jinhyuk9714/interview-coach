package com.interviewcoach.question.infrastructure.llm;

import com.interviewcoach.question.application.dto.request.GenerateQuestionsRequest;
import com.interviewcoach.question.infrastructure.rag.SimilarQuestionResult;

import java.util.List;

public interface LlmClient {

    JdAnalysisResult analyzeJd(String jdText);

    List<GeneratedQuestionResult> generateQuestions(String jdText, List<String> skills,
                                                     String questionType, int count, int difficulty);

    /**
     * RAG 컨텍스트를 활용한 면접 질문 생성
     * 유사 질문을 참고하여 중복 없이 더 높은 품질의 질문 생성
     *
     * @param jdText JD 원문
     * @param skills 추출된 스킬 목록
     * @param questionType 질문 유형 (technical, behavioral, mixed)
     * @param count 생성할 질문 수
     * @param difficulty 난이도 (1-5)
     * @param similarQuestions RAG로 검색된 유사 질문 목록
     * @return 생성된 질문 목록
     */
    List<GeneratedQuestionResult> generateQuestionsWithContext(
            String jdText,
            List<String> skills,
            String questionType,
            int count,
            int difficulty,
            List<SimilarQuestionResult> similarQuestions);

    /**
     * 취약 분야 우선 반영하여 면접 질문 생성
     * 사용자의 취약 카테고리(70% 미만 점수)에서 더 많은 질문을 생성
     *
     * @param jdText JD 원문
     * @param skills 추출된 스킬 목록
     * @param questionType 질문 유형 (technical, behavioral, mixed)
     * @param count 생성할 질문 수
     * @param difficulty 난이도 (1-5)
     * @param similarQuestions RAG로 검색된 유사 질문 목록
     * @param weakCategories 취약 카테고리 목록 (카테고리명, 점수)
     * @return 생성된 질문 목록
     */
    List<GeneratedQuestionResult> generateQuestionsWithWeakAreas(
            String jdText,
            List<String> skills,
            String questionType,
            int count,
            int difficulty,
            List<SimilarQuestionResult> similarQuestions,
            List<GenerateQuestionsRequest.WeakCategoryInfo> weakCategories);

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
