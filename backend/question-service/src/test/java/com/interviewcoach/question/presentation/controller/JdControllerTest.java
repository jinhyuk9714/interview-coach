package com.interviewcoach.question.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.question.application.dto.response.JdAnalysisResponse;
import com.interviewcoach.question.application.dto.response.JdResponse;
import com.interviewcoach.question.application.service.JdService;
import com.interviewcoach.question.exception.GlobalExceptionHandler;
import com.interviewcoach.question.exception.JdNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JdController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("JdController 통합 테스트")
class JdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JdService jdService;

    private static final Long USER_ID = 1L;
    private static final Long JD_ID = 10L;

    @Nested
    @DisplayName("POST /api/v1/jd - JD 생성")
    class CreateJdTest {

        @Test
        @DisplayName("JD 생성 성공 - 201 Created")
        void createJd_Success() throws Exception {
            // given
            JdResponse response = JdResponse.builder()
                    .id(JD_ID)
                    .userId(USER_ID)
                    .companyName("네이버")
                    .position("백엔드 개발자")
                    .originalText("Java, Spring Boot 경력 3년 이상...")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(jdService.createJd(eq(USER_ID), any())).willReturn(response);

            String requestBody = """
                {
                    "companyName": "네이버",
                    "position": "백엔드 개발자",
                    "originalText": "Java, Spring Boot 경력 3년 이상, JPA/Hibernate 경험 우대, PostgreSQL 및 Redis 사용 경험, 마이크로서비스 아키텍처 이해"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/jd")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(JD_ID))
                    .andExpect(jsonPath("$.userId").value(USER_ID))
                    .andExpect(jsonPath("$.companyName").value("네이버"))
                    .andExpect(jsonPath("$.position").value("백엔드 개발자"));
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 에러 - originalText")
        void createJd_MissingOriginalText() throws Exception {
            String requestBody = """
                {
                    "companyName": "네이버",
                    "position": "백엔드 개발자"
                }
                """;

            mockMvc.perform(post("/api/v1/jd")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/jd - JD 목록 조회")
    class GetJdListTest {

        @Test
        @DisplayName("JD 목록 조회 성공 - 200 OK")
        void getJdList_Success() throws Exception {
            // given
            List<JdResponse> response = List.of(
                    JdResponse.builder()
                            .id(1L)
                            .userId(USER_ID)
                            .companyName("네이버")
                            .position("백엔드 개발자")
                            .originalText("Java, Spring Boot...")
                            .createdAt(LocalDateTime.now().minusDays(1))
                            .build(),
                    JdResponse.builder()
                            .id(2L)
                            .userId(USER_ID)
                            .companyName("카카오")
                            .position("서버 개발자")
                            .originalText("Kotlin, Spring WebFlux...")
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            given(jdService.getJdList(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/jd")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].companyName").value("네이버"))
                    .andExpect(jsonPath("$[1].companyName").value("카카오"));
        }

        @Test
        @DisplayName("JD가 없는 사용자 - 빈 목록 반환")
        void getJdList_Empty() throws Exception {
            // given
            given(jdService.getJdList(USER_ID)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/jd")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/jd/{id} - JD 상세 조회")
    class GetJdTest {

        @Test
        @DisplayName("JD 상세 조회 성공 - 200 OK")
        void getJd_Success() throws Exception {
            // given
            JdResponse response = JdResponse.builder()
                    .id(JD_ID)
                    .userId(USER_ID)
                    .companyName("네이버")
                    .position("백엔드 개발자")
                    .originalText("Java, Spring Boot 경력 3년 이상...")
                    .parsedSkills(List.of("Java", "Spring Boot", "JPA"))
                    .parsedRequirements(List.of("3년 이상 경력", "CS 기본기"))
                    .createdAt(LocalDateTime.now())
                    .build();

            given(jdService.getJd(JD_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/jd/{id}", JD_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(JD_ID))
                    .andExpect(jsonPath("$.companyName").value("네이버"))
                    .andExpect(jsonPath("$.parsedSkills").isArray())
                    .andExpect(jsonPath("$.parsedSkills.length()").value(3))
                    .andExpect(jsonPath("$.parsedSkills[0]").value("Java"));
        }

        @Test
        @DisplayName("존재하지 않는 JD 조회 - 404 Not Found")
        void getJd_NotFound() throws Exception {
            // given
            given(jdService.getJd(999L)).willThrow(new JdNotFoundException(999L));

            // when & then
            mockMvc.perform(get("/api/v1/jd/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/jd/{id}/analyze - JD 분석")
    class AnalyzeJdTest {

        @Test
        @DisplayName("JD 분석 성공 - 200 OK")
        void analyzeJd_Success() throws Exception {
            // given
            JdAnalysisResponse response = JdAnalysisResponse.builder()
                    .jdId(JD_ID)
                    .skills(List.of("Java", "Spring Boot", "JPA", "PostgreSQL"))
                    .requirements(List.of("3년 이상 백엔드 경력", "CS 기본기", "협업 능력"))
                    .summary("백엔드 개발자 포지션으로 Java/Spring 기반 서비스 개발 경험이 요구됩니다.")
                    .build();

            given(jdService.analyzeJd(JD_ID)).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/jd/{id}/analyze", JD_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jdId").value(JD_ID))
                    .andExpect(jsonPath("$.skills").isArray())
                    .andExpect(jsonPath("$.skills.length()").value(4))
                    .andExpect(jsonPath("$.requirements").isArray())
                    .andExpect(jsonPath("$.summary").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/jd/{id} - JD 삭제")
    class DeleteJdTest {

        @Test
        @DisplayName("JD 삭제 성공 - 204 No Content")
        void deleteJd_Success() throws Exception {
            // given
            doNothing().when(jdService).deleteJd(USER_ID, JD_ID);

            // when & then
            mockMvc.perform(delete("/api/v1/jd/{id}", JD_ID)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("존재하지 않는 JD 삭제 - 404 Not Found")
        void deleteJd_NotFound() throws Exception {
            // given
            doThrow(new JdNotFoundException(999L)).when(jdService).deleteJd(USER_ID, 999L);

            // when & then
            mockMvc.perform(delete("/api/v1/jd/{id}", 999L)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNotFound());
        }
    }
}
