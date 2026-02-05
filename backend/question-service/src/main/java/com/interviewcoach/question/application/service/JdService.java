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

    public List<JdResponse> getJdList(Long userId) {
        // 캐싱 없이 매번 DB 조회 - 의도적 (6주차 Redis 캐싱으로 최적화)
        return jdRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(JdResponse::from)
                .toList();
    }

    public JdResponse getJd(Long jdId) {
        // 캐싱 없이 매번 DB 조회 - 의도적 (6주차 @Cacheable로 최적화)
        JobDescription jd = jdRepository.findById(jdId)
                .orElseThrow(() -> new JdNotFoundException(jdId));
        return JdResponse.from(jd);
    }

    @Transactional
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
}
