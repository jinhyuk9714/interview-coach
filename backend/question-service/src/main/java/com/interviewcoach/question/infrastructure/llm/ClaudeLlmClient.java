package com.interviewcoach.question.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ClaudeLlmClient implements LlmClient {

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    public ClaudeLlmClient(
            @Value("${langchain4j.anthropic.api-key:}") String apiKey,
            @Value("${langchain4j.anthropic.model-name:claude-3-sonnet-20240229}") String modelName,
            ObjectMapper objectMapper) {

        if (apiKey != null && !apiKey.isBlank()) {
            this.chatModel = AnthropicChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .maxTokens(4096)
                    .build();
            log.info("ClaudeLlmClient initialized with Claude API (model: {})", modelName);
        } else {
            this.chatModel = null;
            log.warn("Claude API key not configured. LLM features will use mock data.");
        }
        this.objectMapper = objectMapper;
    }

    @Override
    public JdAnalysisResult analyzeJd(String jdText) {
        if (chatModel == null) {
            return createMockAnalysisResult();
        }

        String prompt = """
            다음 채용 공고(JD)를 분석해주세요. 응답은 반드시 JSON 형식으로만 해주세요.

            JD 내용:
            %s

            다음 JSON 형식으로 응답해주세요:
            {
                "skills": ["필요한 기술 스택 목록"],
                "requirements": ["자격 요건 목록"],
                "summary": "JD 요약 (2-3문장)"
            }

            JSON만 응답하고 다른 텍스트는 포함하지 마세요.
            """.formatted(jdText);

        try {
            String response = chatModel.generate(prompt);
            return parseAnalysisResponse(response);
        } catch (Exception e) {
            log.error("Failed to analyze JD with Claude: {}", e.getMessage());
            return createMockAnalysisResult();
        }
    }

    @Override
    public List<GeneratedQuestionResult> generateQuestions(String jdText, List<String> skills,
                                                            String questionType, int count, int difficulty) {
        if (chatModel == null) {
            return createMockQuestions(questionType, count, difficulty);
        }

        String difficultyDesc = switch (difficulty) {
            case 1 -> "매우 쉬움 (신입 레벨)";
            case 2 -> "쉬움 (1-2년차)";
            case 3 -> "보통 (3-5년차)";
            case 4 -> "어려움 (5-7년차)";
            case 5 -> "매우 어려움 (시니어/리드)";
            default -> "보통";
        };

        String typeInstruction = switch (questionType) {
            case "technical" -> "기술적인 질문만 생성해주세요 (코딩, 시스템 설계, 알고리즘 등)";
            case "behavioral" -> "행동 면접 질문만 생성해주세요 (경험, 상황 대처, 팀워크 등)";
            default -> "기술 질문과 행동 면접 질문을 섞어서 생성해주세요";
        };

        String prompt = """
            다음 채용 공고와 필요 기술을 바탕으로 면접 질문을 생성해주세요.

            JD 내용:
            %s

            필요 기술: %s

            요청 사항:
            - %s
            - 난이도: %s
            - 질문 개수: %d개

            다음 JSON 배열 형식으로 응답해주세요:
            [
                {
                    "questionType": "technical 또는 behavioral",
                    "skillCategory": "아래 카테고리 중 하나만 선택",
                    "questionText": "면접 질문",
                    "hint": "답변 힌트 (1-2문장)",
                    "idealAnswer": "모범 답변 요약 (2-3문장)",
                    "difficulty": %d
                }
            ]

            skillCategory는 반드시 다음 중 하나만 사용하세요:
            - 기술역량 (코딩, 알고리즘, 자료구조, 프로그래밍 언어 관련)
            - 시스템설계 (아키텍처, 인프라, 확장성, 성능 관련)
            - 문제해결 (트러블슈팅, 디버깅, 분석력 관련)
            - 협업 (팀워크, 커뮤니케이션, 리더십 관련)
            - 프로젝트경험 (실제 프로젝트 경험, 성과 관련)

            JSON 배열만 응답하고 다른 텍스트는 포함하지 마세요.
            """.formatted(jdText, String.join(", ", skills), typeInstruction, difficultyDesc, count, difficulty);

        try {
            String response = chatModel.generate(prompt);
            return parseQuestionsResponse(response);
        } catch (Exception e) {
            log.error("Failed to generate questions with Claude: {}", e.getMessage());
            return createMockQuestions(questionType, count, difficulty);
        }
    }

    private JdAnalysisResult parseAnalysisResponse(String response) {
        try {
            String json = extractJsonObject(response);
            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            List<String> skills = (List<String>) result.getOrDefault("skills", new ArrayList<>());
            @SuppressWarnings("unchecked")
            List<String> requirements = (List<String>) result.getOrDefault("requirements", new ArrayList<>());
            String summary = (String) result.getOrDefault("summary", "");

            return new JdAnalysisResult(skills, requirements, summary);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse analysis response: {}", e.getMessage());
            return createMockAnalysisResult();
        }
    }

    private List<GeneratedQuestionResult> parseQuestionsResponse(String response) {
        try {
            String json = extractJsonArray(response);
            List<Map<String, Object>> questions = objectMapper.readValue(json, new TypeReference<>() {});

            return questions.stream()
                    .map(q -> new GeneratedQuestionResult(
                            (String) q.getOrDefault("questionType", "technical"),
                            (String) q.getOrDefault("skillCategory", "general"),
                            (String) q.getOrDefault("questionText", ""),
                            (String) q.getOrDefault("hint", ""),
                            (String) q.getOrDefault("idealAnswer", ""),
                            ((Number) q.getOrDefault("difficulty", 3)).intValue()
                    ))
                    .toList();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse questions response: {}", e.getMessage());
            return createMockQuestions("mixed", 5, 3);
        }
    }

    private String extractJson(String response) {
        // Try to find JSON object first
        int objStart = response.indexOf('{');
        int objEnd = response.lastIndexOf('}');

        // Try to find JSON array
        int arrStart = response.indexOf('[');
        int arrEnd = response.lastIndexOf(']');

        // Determine which comes first and is valid
        if (objStart != -1 && objEnd != -1 && objEnd > objStart) {
            if (arrStart == -1 || objStart < arrStart) {
                return response.substring(objStart, objEnd + 1);
            }
        }

        if (arrStart != -1 && arrEnd != -1 && arrEnd > arrStart) {
            return response.substring(arrStart, arrEnd + 1);
        }

        return response;
    }

    private String extractJsonObject(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private JdAnalysisResult createMockAnalysisResult() {
        return new JdAnalysisResult(
                List.of("Java", "Spring Boot", "JPA", "PostgreSQL", "Redis"),
                List.of("3년 이상 백엔드 개발 경험", "RESTful API 설계 경험", "협업 및 커뮤니케이션 능력"),
                "백엔드 개발자 포지션으로, Java/Spring 기반 웹 서비스 개발 경험이 필요합니다."
        );
    }

    private List<GeneratedQuestionResult> createMockQuestions(String questionType, int count, int difficulty) {
        List<GeneratedQuestionResult> questions = new ArrayList<>();

        // 카테고리: 기술역량, 시스템설계, 문제해결, 협업, 프로젝트경험
        List<GeneratedQuestionResult> technicalQuestions = List.of(
                new GeneratedQuestionResult("technical", "기술역량",
                        "Java의 가비지 컬렉션(GC) 동작 원리와 GC 튜닝 경험에 대해 설명해주세요.",
                        "Young/Old Generation, GC 알고리즘 종류를 언급하세요",
                        "Java GC는 Heap 메모리를 Young과 Old Generation으로 나누어 관리합니다.", difficulty),
                new GeneratedQuestionResult("technical", "기술역량",
                        "Spring의 IoC/DI 개념과 실제 프로젝트에서 어떻게 활용했는지 설명해주세요.",
                        "의존성 주입의 장점과 테스트 용이성을 언급하세요",
                        "IoC는 제어의 역전으로, 객체 생성과 생명주기를 컨테이너가 관리합니다.", difficulty),
                new GeneratedQuestionResult("technical", "시스템설계",
                        "대용량 트래픽을 처리하기 위한 시스템 설계 경험이 있다면 설명해주세요.",
                        "캐싱, 로드밸런싱, 비동기 처리 등을 언급하세요",
                        "대용량 트래픽 처리를 위해 Redis 캐싱과 메시지 큐를 활용했습니다.", difficulty)
        );

        List<GeneratedQuestionResult> behavioralQuestions = List.of(
                new GeneratedQuestionResult("behavioral", "문제해결",
                        "기술적으로 어려운 문제를 해결한 경험에 대해 말씀해주세요.",
                        "STAR 기법으로 구체적인 상황과 행동, 결과를 설명하세요",
                        "문제 상황, 분석 과정, 해결 방법, 결과를 체계적으로 설명합니다.", difficulty),
                new GeneratedQuestionResult("behavioral", "협업",
                        "팀원과 의견 충돌이 있었던 경험과 어떻게 해결했는지 설명해주세요.",
                        "경청, 논리적 설득, 합의점 도출 과정을 설명하세요",
                        "의견 충돌 시 상대방 의견을 경청하고 데이터 기반으로 논의했습니다.", difficulty)
        );

        if ("technical".equals(questionType)) {
            for (int i = 0; i < count && i < technicalQuestions.size(); i++) {
                questions.add(technicalQuestions.get(i));
            }
        } else if ("behavioral".equals(questionType)) {
            for (int i = 0; i < count && i < behavioralQuestions.size(); i++) {
                questions.add(behavioralQuestions.get(i));
            }
        } else {
            int techCount = count / 2;
            int behavCount = count - techCount;
            for (int i = 0; i < techCount && i < technicalQuestions.size(); i++) {
                questions.add(technicalQuestions.get(i));
            }
            for (int i = 0; i < behavCount && i < behavioralQuestions.size(); i++) {
                questions.add(behavioralQuestions.get(i));
            }
        }

        return questions;
    }
}
