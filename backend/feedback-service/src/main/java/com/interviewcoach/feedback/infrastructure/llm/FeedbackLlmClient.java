package com.interviewcoach.feedback.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.feedback.application.dto.response.FeedbackResponse;
import com.interviewcoach.feedback.application.dto.response.FollowUpQuestion;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FeedbackLlmClient {

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    public FeedbackLlmClient(
            @Value("${langchain4j.anthropic.api-key:}") String apiKey,
            @Value("${langchain4j.anthropic.model-name:claude-sonnet-4-20250514}") String modelName,
            ObjectMapper objectMapper) {

        if (apiKey != null && !apiKey.isBlank()) {
            this.chatModel = AnthropicChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .maxTokens(2048)
                    .build();
            log.info("FeedbackLlmClient initialized with Claude API");
        } else {
            this.chatModel = null;
            log.warn("Claude API key not configured. Feedback will use mock data.");
        }
        this.objectMapper = objectMapper;
    }

    private static final int MAX_FOLLOW_UP_DEPTH = 2;
    private static final int FOLLOW_UP_SCORE_THRESHOLD = 85;

    public FeedbackResponse generateFeedback(Long sessionId, Long qnaId, String questionText, String answerText) {
        return generateFeedbackWithFollowUp(sessionId, qnaId, questionText, answerText, 0);
    }

    public FeedbackResponse generateFeedbackWithFollowUp(Long sessionId, Long qnaId, String questionText, String answerText, int followUpDepth) {
        if (chatModel == null) {
            return createMockFeedbackWithFollowUp(sessionId, qnaId, followUpDepth);
        }

        String prompt = """
            다음 면접 질문과 답변을 평가하고, 꼬리 질문을 생성해주세요.

            질문: %s

            답변: %s

            현재 꼬리 질문 깊이: %d (최대 %d)

            다음 JSON 형식으로 응답해주세요:
            {
                "feedback": {
                    "score": 0-100 사이 점수,
                    "strengths": ["강점 1", "강점 2"],
                    "improvements": ["개선점 1", "개선점 2"],
                    "tips": "답변 개선을 위한 팁 (1-2문장)",
                    "overallComment": "전체적인 평가 코멘트 (2-3문장)"
                },
                "followUpQuestion": {
                    "questionText": "꼬리 질문 내용",
                    "focusArea": "탐구 영역 (구체적 사례, 기술적 깊이, 결과/임팩트, 문제 해결 과정 중 하나)",
                    "rationale": "이 질문을 하는 이유 (1문장)",
                    "shouldAsk": true 또는 false
                }
            }

            평가 기준:
            - 질문에 대한 이해도
            - 답변의 구체성과 논리성
            - 실제 경험/예시 포함 여부
            - STAR 기법 활용 여부 (상황-과제-행동-결과)

            꼬리 질문 생성 기준:
            - 답변이 모호하거나 구체성이 부족하면 shouldAsk: true
            - 점수가 %d점 이상이면서 깊이가 %d 이상이면 shouldAsk: false
            - 꼬리 질문 유형: 구체적 사례 요청, 기술적 깊이 탐구, 결과/임팩트 확인, 문제 해결 과정 파악

            JSON만 응답하고 다른 텍스트는 포함하지 마세요.
            """.formatted(questionText, answerText, followUpDepth, MAX_FOLLOW_UP_DEPTH,
                         FOLLOW_UP_SCORE_THRESHOLD, MAX_FOLLOW_UP_DEPTH);

        try {
            String response = chatModel.generate(prompt);
            return parseFeedbackWithFollowUpResponse(sessionId, qnaId, response, followUpDepth);
        } catch (Exception e) {
            log.error("Failed to generate feedback with Claude: {}", e.getMessage());
            return createMockFeedbackWithFollowUp(sessionId, qnaId, followUpDepth);
        }
    }

    private FeedbackResponse parseFeedbackResponse(Long sessionId, Long qnaId, String response) {
        try {
            String json = extractJsonObject(response);
            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});

            int score = ((Number) result.getOrDefault("score", 75)).intValue();

            @SuppressWarnings("unchecked")
            List<String> strengths = (List<String>) result.getOrDefault("strengths", List.of("답변이 제출되었습니다"));

            @SuppressWarnings("unchecked")
            List<String> improvements = (List<String>) result.getOrDefault("improvements", List.of("더 구체적인 예시를 들어주세요"));

            String tipsStr = (String) result.getOrDefault("tips", "");
            List<String> tips = tipsStr.isEmpty() ? List.of() : List.of(tipsStr);
            String overallComment = (String) result.getOrDefault("overallComment", "");

            return FeedbackResponse.builder()
                    .sessionId(sessionId)
                    .qnaId(qnaId)
                    .score(score)
                    .strengths(strengths)
                    .improvements(improvements)
                    .tips(tips)
                    .overallComment(overallComment)
                    .hasFollowUp(false)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse feedback response: {}", e.getMessage());
            return createMockFeedback(sessionId, qnaId);
        }
    }

    private FeedbackResponse parseFeedbackWithFollowUpResponse(Long sessionId, Long qnaId, String response, int followUpDepth) {
        try {
            String json = extractJsonObject(response);
            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            Map<String, Object> feedbackMap = (Map<String, Object>) result.getOrDefault("feedback", result);

            int score = ((Number) feedbackMap.getOrDefault("score", 75)).intValue();

            @SuppressWarnings("unchecked")
            List<String> strengths = (List<String>) feedbackMap.getOrDefault("strengths", List.of("답변이 제출되었습니다"));

            @SuppressWarnings("unchecked")
            List<String> improvements = (List<String>) feedbackMap.getOrDefault("improvements", List.of("더 구체적인 예시를 들어주세요"));

            String tipsStr = (String) feedbackMap.getOrDefault("tips", "");
            List<String> tips = tipsStr.isEmpty() ? List.of() : List.of(tipsStr);
            String overallComment = (String) feedbackMap.getOrDefault("overallComment", "");

            FollowUpQuestion followUpQuestion = null;
            boolean hasFollowUp = false;

            @SuppressWarnings("unchecked")
            Map<String, Object> followUpMap = (Map<String, Object>) result.get("followUpQuestion");
            if (followUpMap != null) {
                boolean shouldAsk = Boolean.TRUE.equals(followUpMap.get("shouldAsk"));

                if (shouldAsk && followUpDepth < MAX_FOLLOW_UP_DEPTH && score < FOLLOW_UP_SCORE_THRESHOLD) {
                    followUpQuestion = FollowUpQuestion.builder()
                            .questionText((String) followUpMap.getOrDefault("questionText", ""))
                            .focusArea((String) followUpMap.getOrDefault("focusArea", ""))
                            .rationale((String) followUpMap.getOrDefault("rationale", ""))
                            .shouldAsk(true)
                            .build();
                    hasFollowUp = true;
                    log.info("Generated follow-up question for qnaId={}, depth={}", qnaId, followUpDepth);
                }
            }

            return FeedbackResponse.builder()
                    .sessionId(sessionId)
                    .qnaId(qnaId)
                    .score(score)
                    .strengths(strengths)
                    .improvements(improvements)
                    .tips(tips)
                    .overallComment(overallComment)
                    .followUpQuestion(followUpQuestion)
                    .hasFollowUp(hasFollowUp)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse feedback with follow-up response: {}", e.getMessage());
            return createMockFeedbackWithFollowUp(sessionId, qnaId, followUpDepth);
        }
    }

    private String extractJsonObject(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private FeedbackResponse createMockFeedback(Long sessionId, Long qnaId) {
        return FeedbackResponse.builder()
                .sessionId(sessionId)
                .qnaId(qnaId)
                .score(75)
                .strengths(List.of(
                        "핵심 개념을 정확하게 이해하고 있습니다",
                        "실제 경험을 바탕으로 설명했습니다"
                ))
                .improvements(List.of(
                        "더 구체적인 예시를 들어 설명하면 좋겠습니다",
                        "기술적 깊이를 더 보여주세요"
                ))
                .tips(List.of("답변 시 STAR 기법(상황-과제-행동-결과)을 활용하면 더 체계적인 답변이 됩니다."))
                .overallComment("전반적으로 좋은 답변이지만, 구체적인 수치와 결과를 포함하면 더 설득력이 있을 것입니다.")
                .hasFollowUp(false)
                .build();
    }

    private FeedbackResponse createMockFeedbackWithFollowUp(Long sessionId, Long qnaId, int followUpDepth) {
        int score = 72;
        boolean shouldGenerateFollowUp = followUpDepth < MAX_FOLLOW_UP_DEPTH && score < FOLLOW_UP_SCORE_THRESHOLD;

        FollowUpQuestion followUpQuestion = null;
        if (shouldGenerateFollowUp) {
            followUpQuestion = FollowUpQuestion.builder()
                    .questionText("그 상황에서 구체적으로 어떤 행동을 취하셨나요? 결과는 어떠했나요?")
                    .focusArea("구체적 사례")
                    .rationale("답변에서 언급한 경험의 구체적인 행동과 결과를 확인하기 위해")
                    .shouldAsk(true)
                    .build();
        }

        return FeedbackResponse.builder()
                .sessionId(sessionId)
                .qnaId(qnaId)
                .score(score)
                .strengths(List.of(
                        "핵심 개념을 정확하게 이해하고 있습니다",
                        "실제 경험을 바탕으로 설명했습니다"
                ))
                .improvements(List.of(
                        "더 구체적인 예시를 들어 설명하면 좋겠습니다",
                        "기술적 깊이를 더 보여주세요"
                ))
                .tips(List.of("답변 시 STAR 기법(상황-과제-행동-결과)을 활용하면 더 체계적인 답변이 됩니다."))
                .overallComment("전반적으로 좋은 답변이지만, 구체적인 수치와 결과를 포함하면 더 설득력이 있을 것입니다.")
                .followUpQuestion(followUpQuestion)
                .hasFollowUp(shouldGenerateFollowUp)
                .build();
    }
}
