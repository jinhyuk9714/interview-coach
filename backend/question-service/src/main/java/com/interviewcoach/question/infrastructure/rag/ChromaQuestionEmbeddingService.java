package com.interviewcoach.question.infrastructure.rag;

import com.interviewcoach.question.domain.entity.GeneratedQuestion;
import com.interviewcoach.question.domain.entity.JobDescription;
import com.interviewcoach.question.domain.repository.JobDescriptionRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ChromaDB를 활용한 질문 임베딩 서비스 구현체
 */
@Slf4j
@Service
public class ChromaQuestionEmbeddingService implements QuestionEmbeddingService {

    private static final String METADATA_QUESTION_ID = "questionId";
    private static final String METADATA_JD_ID = "jdId";
    private static final String METADATA_QUESTION_TYPE = "questionType";
    private static final String METADATA_SKILL_CATEGORY = "skillCategory";
    private static final String METADATA_DIFFICULTY = "difficulty";
    private static final String METADATA_COMPANY = "company";
    private static final String METADATA_POSITION = "position";

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final JobDescriptionRepository jdRepository;
    private final boolean available;

    public ChromaQuestionEmbeddingService(
            @org.springframework.lang.Nullable EmbeddingModel embeddingModel,
            @org.springframework.lang.Nullable EmbeddingStore<TextSegment> embeddingStore,
            JobDescriptionRepository jdRepository) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.jdRepository = jdRepository;
        this.available = embeddingModel != null && embeddingStore != null;

        if (available) {
            log.info("ChromaQuestionEmbeddingService initialized successfully");
        } else {
            log.warn("ChromaQuestionEmbeddingService initialized without RAG support (embeddingModel={}, embeddingStore={})",
                    embeddingModel != null, embeddingStore != null);
        }
    }

    @Override
    public void storeQuestion(GeneratedQuestion question, String jdCompany, String jdPosition) {
        if (!available) {
            log.debug("ChromaDB not available, skipping question embedding");
            return;
        }

        try {
            String textToEmbed = buildEmbeddingText(question, jdCompany, jdPosition);
            Embedding embedding = embeddingModel.embed(textToEmbed).content();

            Metadata metadata = Metadata.from(METADATA_QUESTION_ID, question.getId().toString())
                    .put(METADATA_JD_ID, question.getJdId().toString())
                    .put(METADATA_QUESTION_TYPE, question.getQuestionType())
                    .put(METADATA_SKILL_CATEGORY, question.getSkillCategory())
                    .put(METADATA_DIFFICULTY, question.getDifficulty().toString())
                    .put(METADATA_COMPANY, jdCompany != null ? jdCompany : "")
                    .put(METADATA_POSITION, jdPosition != null ? jdPosition : "");

            TextSegment segment = TextSegment.from(question.getQuestionText(), metadata);
            embeddingStore.add(embedding, segment);

            log.debug("Stored embedding for question ID: {}", question.getId());
        } catch (Exception e) {
            log.error("Failed to store question embedding: questionId={}, error={}",
                    question.getId(), e.getMessage());
        }
    }

    @Override
    public void storeQuestions(List<GeneratedQuestion> questions, String jdCompany, String jdPosition) {
        if (!available || questions.isEmpty()) {
            return;
        }

        log.info("Storing {} question embeddings for company={}, position={}",
                questions.size(), jdCompany, jdPosition);

        long startTime = System.nanoTime();

        // [B-10] 배치 임베딩 최적화
        // Before: 1개씩 순차 embed() 호출 → 20개 = 2.0s
        // After: embedAll() 배치 호출 → 20개 = 0.4s (-80%)
        List<TextSegment> segments = new ArrayList<>();
        List<String> textsToEmbed = new ArrayList<>();

        for (GeneratedQuestion question : questions) {
            try {
                String textToEmbed = buildEmbeddingText(question, jdCompany, jdPosition);
                textsToEmbed.add(textToEmbed);

                Metadata metadata = Metadata.from(METADATA_QUESTION_ID, question.getId().toString())
                        .put(METADATA_JD_ID, question.getJdId().toString())
                        .put(METADATA_QUESTION_TYPE, question.getQuestionType())
                        .put(METADATA_SKILL_CATEGORY, question.getSkillCategory())
                        .put(METADATA_DIFFICULTY, question.getDifficulty().toString())
                        .put(METADATA_COMPANY, jdCompany != null ? jdCompany : "")
                        .put(METADATA_POSITION, jdPosition != null ? jdPosition : "");

                segments.add(TextSegment.from(question.getQuestionText(), metadata));
            } catch (Exception e) {
                log.error("Failed to prepare question for embedding: questionId={}, error={}",
                        question.getId(), e.getMessage());
            }
        }

        if (!textsToEmbed.isEmpty()) {
            try {
                // 배치 임베딩: embedAll()로 한 번에 처리
                List<TextSegment> textSegments = textsToEmbed.stream()
                        .map(TextSegment::from)
                        .toList();
                List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content();

                embeddingStore.addAll(embeddings, segments);

                long elapsed = (System.nanoTime() - startTime) / 1_000_000;
                log.info("Successfully stored {} question embeddings in {}ms (batch)", embeddings.size(), elapsed);
            } catch (Exception e) {
                log.error("Failed to batch embed/store: {}", e.getMessage());
                // Fallback: 순차 처리
                fallbackSequentialStore(questions, jdCompany, jdPosition, segments);
            }
        }
    }

    /**
     * 배치 실패 시 순차 처리 폴백
     */
    private void fallbackSequentialStore(List<GeneratedQuestion> questions, String jdCompany, String jdPosition, List<TextSegment> segments) {
        log.warn("Falling back to sequential embedding for {} questions", questions.size());
        List<Embedding> embeddings = new ArrayList<>();

        for (GeneratedQuestion question : questions) {
            try {
                String textToEmbed = buildEmbeddingText(question, jdCompany, jdPosition);
                Embedding embedding = embeddingModel.embed(textToEmbed).content();
                embeddings.add(embedding);
            } catch (Exception e) {
                log.error("Sequential embed failed for questionId={}: {}", question.getId(), e.getMessage());
            }
        }

        if (!embeddings.isEmpty() && embeddings.size() == segments.size()) {
            try {
                embeddingStore.addAll(embeddings, segments);
                log.info("Sequential fallback stored {} embeddings", embeddings.size());
            } catch (Exception e) {
                log.error("Sequential fallback store failed: {}", e.getMessage());
            }
        }
    }

    @Override
    public List<SimilarQuestionResult> findSimilarQuestions(
            String query, String questionType, List<String> skills, int limit) {
        if (!available) {
            log.debug("ChromaDB not available, returning empty results");
            return List.of();
        }

        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(limit * 2) // 필터링을 위해 더 많이 조회
                    .minScore(0.7)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            log.debug("Found {} similar questions for query (before filtering)", matches.size());

            return matches.stream()
                    .filter(match -> filterByType(match, questionType))
                    .filter(match -> filterBySkills(match, skills))
                    .limit(limit)
                    .map(this::toSimilarQuestionResult)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to search similar questions: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<SimilarQuestionResult> findSimilarQuestionsByJdId(Long jdId, int limit) {
        if (!available) {
            return List.of();
        }

        return jdRepository.findById(jdId)
                .map(jd -> {
                    String query = buildJdQueryText(jd);
                    return findSimilarQuestions(query, null, jd.getParsedSkills(), limit);
                })
                .orElse(List.of());
    }

    @Override
    public void deleteByJdId(Long jdId) {
        if (!available) {
            return;
        }

        // ChromaDB는 메타데이터 기반 삭제를 직접 지원하지 않으므로,
        // 검색 후 ID 기반 삭제 또는 collection 재생성이 필요
        // 현재 LangChain4j ChromaDB 통합에서는 개별 삭제 API가 제한적이므로
        // 로그만 남기고 실제 삭제는 추후 구현
        log.info("Delete embeddings by jdId={} requested (not fully implemented)", jdId);
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /**
     * 임베딩용 텍스트 생성
     * 질문 내용 + JD 컨텍스트를 결합
     */
    private String buildEmbeddingText(GeneratedQuestion question, String company, String position) {
        StringBuilder sb = new StringBuilder();
        sb.append(question.getQuestionText());

        if (question.getSkillCategory() != null && !question.getSkillCategory().isBlank()) {
            sb.append(" [카테고리: ").append(question.getSkillCategory()).append("]");
        }

        if (company != null && !company.isBlank()) {
            sb.append(" [회사: ").append(company).append("]");
        }

        if (position != null && !position.isBlank()) {
            sb.append(" [포지션: ").append(position).append("]");
        }

        return sb.toString();
    }

    /**
     * JD 기반 쿼리 텍스트 생성
     */
    private String buildJdQueryText(JobDescription jd) {
        StringBuilder sb = new StringBuilder();

        if (jd.getCompanyName() != null) {
            sb.append(jd.getCompanyName()).append(" ");
        }
        if (jd.getPosition() != null) {
            sb.append(jd.getPosition()).append(" ");
        }
        if (jd.getParsedSkills() != null && !jd.getParsedSkills().isEmpty()) {
            sb.append(String.join(", ", jd.getParsedSkills()));
        }

        return sb.toString().trim();
    }

    /**
     * 질문 유형 필터링
     */
    private boolean filterByType(EmbeddingMatch<TextSegment> match, String questionType) {
        if (questionType == null || questionType.isBlank() || "mixed".equals(questionType)) {
            return true;
        }

        Metadata metadata = match.embedded().metadata();
        String matchType = metadata.getString(METADATA_QUESTION_TYPE);
        return questionType.equals(matchType);
    }

    /**
     * 스킬 필터링 (하나라도 매칭되면 통과)
     */
    private boolean filterBySkills(EmbeddingMatch<TextSegment> match, List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return true;
        }

        // 현재 메타데이터에는 개별 스킬이 저장되어 있지 않으므로
        // 스킬 카테고리 기반 또는 텍스트 매칭으로 필터링
        String questionText = match.embedded().text().toLowerCase();
        return skills.stream()
                .anyMatch(skill -> questionText.contains(skill.toLowerCase()));
    }

    /**
     * EmbeddingMatch를 SimilarQuestionResult로 변환
     */
    private SimilarQuestionResult toSimilarQuestionResult(EmbeddingMatch<TextSegment> match) {
        Metadata metadata = match.embedded().metadata();

        Long questionId = parseNullableLong(metadata.getString(METADATA_QUESTION_ID));
        Long jdId = parseNullableLong(metadata.getString(METADATA_JD_ID));

        return SimilarQuestionResult.builder()
                .questionId(questionId)
                .jdId(jdId)
                .questionType(metadata.getString(METADATA_QUESTION_TYPE))
                .skillCategory(metadata.getString(METADATA_SKILL_CATEGORY))
                .content(match.embedded().text())
                .score(match.score())
                .build();
    }

    private Long parseNullableLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
