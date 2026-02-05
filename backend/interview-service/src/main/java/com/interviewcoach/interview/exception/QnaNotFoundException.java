package com.interviewcoach.interview.exception;

public class QnaNotFoundException extends RuntimeException {

    public QnaNotFoundException(Long sessionId, Integer questionOrder) {
        super("질문을 찾을 수 없습니다. 세션: " + sessionId + ", 순서: " + questionOrder);
    }
}
