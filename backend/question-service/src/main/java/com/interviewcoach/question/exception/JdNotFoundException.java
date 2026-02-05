package com.interviewcoach.question.exception;

public class JdNotFoundException extends RuntimeException {

    public JdNotFoundException(Long jdId) {
        super("JD를 찾을 수 없습니다: " + jdId);
    }
}
