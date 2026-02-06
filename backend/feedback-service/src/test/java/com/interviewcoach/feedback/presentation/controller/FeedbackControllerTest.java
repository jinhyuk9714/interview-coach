package com.interviewcoach.feedback.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.feedback.application.service.FeedbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedbackController.class)
@DisplayName("FeedbackController 통합 테스트")
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeedbackService feedbackService;

    private static final Long SESSION_ID = 1L;

    @Nested
    @DisplayName("GET /api/v1/feedback/session/{sessionId}/stream - SSE 피드백 스트림")
    class GetStreamFeedbackTest {

        @Test
        @DisplayName("GET SSE 스트림 엔드포인트 - text/event-stream 반환")
        void streamFeedback_ReturnsSseContentType() throws Exception {
            // given
            SseEmitter mockEmitter = new SseEmitter(60000L);
            given(feedbackService.streamFeedback(
                    eq(SESSION_ID), eq(10L), eq("질문입니다"), eq("답변입니다"), eq(0)))
                    .willReturn(mockEmitter);

            // when & then
            mockMvc.perform(get("/api/v1/feedback/session/{sessionId}/stream", SESSION_ID)
                            .param("qnaId", "10")
                            .param("question", "질문입니다")
                            .param("answer", "답변입니다")
                            .param("followUpDepth", "0")
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET SSE 스트림 - optional 파라미터 없이 호출")
        void streamFeedback_WithoutOptionalParams() throws Exception {
            // given
            SseEmitter mockEmitter = new SseEmitter(60000L);
            given(feedbackService.streamFeedback(
                    eq(SESSION_ID), isNull(), isNull(), isNull(), eq(0)))
                    .willReturn(mockEmitter);

            // when & then
            mockMvc.perform(get("/api/v1/feedback/session/{sessionId}/stream", SESSION_ID)
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/feedback/session/{sessionId}/stream - SSE 피드백 스트림 (POST)")
    class PostStreamFeedbackTest {

        @Test
        @DisplayName("POST SSE 스트림 엔드포인트 - text/event-stream 반환")
        void streamFeedbackPost_ReturnsSseContentType() throws Exception {
            // given
            SseEmitter mockEmitter = new SseEmitter(60000L);
            given(feedbackService.streamFeedback(
                    eq(SESSION_ID), eq(10L), eq("질문입니다"), eq("긴 답변 내용입니다..."), eq(0)))
                    .willReturn(mockEmitter);

            String requestBody = """
                {
                    "qnaId": 10,
                    "question": "질문입니다",
                    "answer": "긴 답변 내용입니다...",
                    "followUpDepth": 0
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/feedback/session/{sessionId}/stream", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST SSE 스트림 - followUpDepth null일 때 기본값 0")
        void streamFeedbackPost_NullFollowUpDepth() throws Exception {
            // given
            SseEmitter mockEmitter = new SseEmitter(60000L);
            given(feedbackService.streamFeedback(
                    eq(SESSION_ID), eq(10L), eq("질문입니다"), eq("답변입니다"), eq(0)))
                    .willReturn(mockEmitter);

            String requestBody = """
                {
                    "qnaId": 10,
                    "question": "질문입니다",
                    "answer": "답변입니다"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/feedback/session/{sessionId}/stream", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST SSE 스트림 - 요청 본문 없이 호출 시 400 에러")
        void streamFeedbackPost_MissingBody() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/feedback/session/{sessionId}/stream", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isBadRequest());
        }
    }
}
