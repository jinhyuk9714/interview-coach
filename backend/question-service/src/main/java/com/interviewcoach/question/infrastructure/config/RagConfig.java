package com.interviewcoach.question.infrastructure.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG (Retrieval-Augmented Generation) 관련 Bean 설정
 *
 * - EmbeddingModel: AllMiniLmL6V2 (로컬 모델, API 비용 없음)
 * - EmbeddingStore: ChromaDB
 * - ContentRetriever: EmbeddingStore 기반 검색기
 */
@Slf4j
@Configuration
public class RagConfig {

    @Value("${langchain4j.chroma.base-url:http://localhost:8000}")
    private String chromaBaseUrl;

    @Value("${langchain4j.chroma.collection-name:interview-questions}")
    private String collectionName;

    /**
     * 로컬 임베딩 모델 (AllMiniLmL6V2)
     * - 384차원 벡터 생성
     * - API 호출 없이 로컬에서 실행
     * - 네이티브 라이브러리 로드 실패 시 null 반환 (RAG 비활성화)
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        try {
            log.info("Initializing AllMiniLmL6V2 embedding model (local)");
            return new AllMiniLmL6V2EmbeddingModel();
        } catch (Exception | UnsatisfiedLinkError e) {
            log.warn("Failed to initialize embedding model: {}. RAG features will be disabled.", e.getMessage());
            return null;
        }
    }

    /**
     * ChromaDB 벡터 저장소
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("Connecting to ChromaDB at {} with collection '{}'", chromaBaseUrl, collectionName);
        try {
            ChromaEmbeddingStore store = ChromaEmbeddingStore.builder()
                    .baseUrl(chromaBaseUrl)
                    .collectionName(collectionName)
                    .build();
            log.info("ChromaDB connection established successfully");
            return store;
        } catch (Exception e) {
            log.warn("Failed to connect to ChromaDB: {}. RAG features will be disabled.", e.getMessage());
            return null;
        }
    }

    /**
     * 임베딩 기반 콘텐츠 검색기
     */
    @Bean
    public ContentRetriever contentRetriever(
            @org.springframework.lang.Nullable EmbeddingModel embeddingModel,
            @org.springframework.lang.Nullable EmbeddingStore<TextSegment> embeddingStore) {
        if (embeddingModel == null || embeddingStore == null) {
            log.warn("EmbeddingModel or EmbeddingStore is null. ContentRetriever will not be available.");
            return null;
        }

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.7)
                .build();
    }
}
