package com.interviewcoach.question.infrastructure.rag;

import com.interviewcoach.question.domain.entity.GeneratedQuestion;

import java.util.List;

/**
 * 질문 임베딩 서비스 인터페이스
 * ChromaDB를 활용한 벡터 저장 및 유사 질문 검색
 */
public interface QuestionEmbeddingService {

    /**
     * 단일 질문을 벡터로 변환하여 ChromaDB에 저장
     *
     * @param question 저장할 질문
     * @param jdCompany JD의 회사명 (메타데이터)
     * @param jdPosition JD의 포지션 (메타데이터)
     */
    void storeQuestion(GeneratedQuestion question, String jdCompany, String jdPosition);

    /**
     * 다수의 질문을 일괄 저장
     *
     * @param questions 저장할 질문 목록
     * @param jdCompany JD의 회사명
     * @param jdPosition JD의 포지션
     */
    void storeQuestions(List<GeneratedQuestion> questions, String jdCompany, String jdPosition);

    /**
     * 쿼리 텍스트 기반 유사 질문 검색
     *
     * @param query 검색 쿼리 (JD 텍스트 또는 질문)
     * @param questionType 질문 유형 필터 (null이면 필터링 안함)
     * @param skills 스킬 필터 (null이면 필터링 안함)
     * @param limit 최대 결과 수
     * @return 유사 질문 목록 (점수 내림차순)
     */
    List<SimilarQuestionResult> findSimilarQuestions(
            String query,
            String questionType,
            List<String> skills,
            int limit);

    /**
     * JD ID 기반 해당 JD와 유사한 질문 검색
     *
     * @param jdId JD ID
     * @param limit 최대 결과 수
     * @return 유사 질문 목록
     */
    List<SimilarQuestionResult> findSimilarQuestionsByJdId(Long jdId, int limit);

    /**
     * JD ID에 해당하는 모든 임베딩 삭제
     *
     * @param jdId 삭제할 JD ID
     */
    void deleteByJdId(Long jdId);

    /**
     * RAG 서비스가 사용 가능한지 확인
     *
     * @return ChromaDB 연결 상태
     */
    boolean isAvailable();
}
