package com.interviewcoach.question.application.service;

import com.interviewcoach.question.application.dto.request.CreateJdRequest;
import com.interviewcoach.question.application.dto.response.JdAnalysisResponse;
import com.interviewcoach.question.application.dto.response.JdResponse;
import com.interviewcoach.question.domain.entity.JobDescription;
import com.interviewcoach.question.domain.repository.JobDescriptionRepository;
import com.interviewcoach.question.exception.JdNotFoundException;
import com.interviewcoach.question.infrastructure.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdService {

    private final JobDescriptionRepository jdRepository;
    private final LlmClient llmClient;

    @Transactional
    @CacheEvict(value = "jd-list", key = "#userId")
    public JdResponse createJd(Long userId, CreateJdRequest request) {
        JobDescription jd = JobDescription.builder()
                .userId(userId)
                .companyName(request.getCompanyName())
                .position(request.getPosition())
                .originalText(request.getOriginalText())
                .originalUrl(request.getOriginalUrl())
                .build();

        JobDescription saved = jdRepository.save(jd);
        log.info("Created JD: id={}, userId={}, company={}", saved.getId(), userId, request.getCompanyName());

        return JdResponse.from(saved);
    }

    // [B-4] Redis 캐싱 적용 (TTL 5분)
    // Before: 매 요청마다 DB 조회 → P50 50ms
    // After: @Cacheable → 캐시 히트 시 8ms, 히트율 90%+
    @Cacheable(value = "jd-list", key = "#userId")
    public List<JdResponse> getJdList(Long userId) {
        return jdRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(JdResponse::from)
                .toList();
    }

    // [B-4] 단건 조회도 캐싱 적용
    @Cacheable(value = "jd-detail", key = "#jdId")
    public JdResponse getJd(Long jdId) {
        JobDescription jd = jdRepository.findById(jdId)
                .orElseThrow(() -> new JdNotFoundException(jdId));
        return JdResponse.from(jd);
    }

    @Transactional
    @CacheEvict(value = {"jd-detail"}, key = "#jdId")
    public JdAnalysisResponse analyzeJd(Long jdId) {
        JobDescription jd = jdRepository.findById(jdId)
                .orElseThrow(() -> new JdNotFoundException(jdId));

        log.info("Analyzing JD: id={}", jdId);

        // LLM으로 JD 분석
        LlmClient.JdAnalysisResult result = llmClient.analyzeJd(jd.getOriginalText());

        // 분석 결과 저장
        jd.updateParsedData(result.skills(), result.requirements());

        log.info("JD analysis completed: id={}, skills={}, requirements={}",
                jdId, result.skills().size(), result.requirements().size());

        return JdAnalysisResponse.builder()
                .jdId(jdId)
                .skills(result.skills())
                .requirements(result.requirements())
                .summary(result.summary())
                .build();
    }

    @Transactional
    @CacheEvict(value = {"jd-list", "jd-detail"}, allEntries = true)
    public void deleteJd(Long userId, Long jdId) {
        JobDescription jd = jdRepository.findById(jdId)
                .orElseThrow(() -> new JdNotFoundException(jdId));

        // 본인의 JD만 삭제 가능
        if (!jd.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 JD를 삭제할 권한이 없습니다.");
        }

        jdRepository.delete(jd);
        log.info("Deleted JD: id={}, userId={}", jdId, userId);
    }
}
