-- Interview Coach Database Schema

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    target_position VARCHAR(100),
    experience_years INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Job Descriptions table
CREATE TABLE IF NOT EXISTS job_descriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(100),
    position VARCHAR(100),
    original_text TEXT NOT NULL,
    original_url VARCHAR(500),
    parsed_skills JSONB DEFAULT '[]',
    parsed_requirements JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Interview Sessions table
CREATE TABLE IF NOT EXISTS interview_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    jd_id BIGINT REFERENCES job_descriptions(id) ON DELETE SET NULL,
    interview_type VARCHAR(50) NOT NULL, -- 'technical', 'behavioral', 'mixed'
    status VARCHAR(20) DEFAULT 'in_progress', -- 'in_progress', 'completed', 'cancelled'
    total_questions INT DEFAULT 0,
    avg_score DECIMAL(3,1),
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Interview Q&A table
CREATE TABLE IF NOT EXISTS interview_qna (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES interview_sessions(id) ON DELETE CASCADE,
    question_order INT NOT NULL,
    question_type VARCHAR(50), -- 'technical', 'behavioral', 'follow_up'
    question_text TEXT NOT NULL,
    answer_text TEXT,
    feedback JSONB, -- {score, strengths, improvements, tips}
    answered_at TIMESTAMP,
    parent_qna_id BIGINT,
    follow_up_depth INT DEFAULT 0,
    is_follow_up BOOLEAN DEFAULT false
);

-- User Statistics table
CREATE TABLE IF NOT EXISTS user_statistics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    skill_category VARCHAR(50) NOT NULL,
    total_questions INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    total_score INT DEFAULT 0,
    correct_rate DECIMAL(5,2) DEFAULT 0,
    weak_points JSONB DEFAULT '[]',
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, skill_category)
);

-- Generated Questions table
CREATE TABLE IF NOT EXISTS generated_questions (
    id BIGSERIAL PRIMARY KEY,
    jd_id BIGINT REFERENCES job_descriptions(id) ON DELETE CASCADE,
    question_type VARCHAR(50),
    skill_category VARCHAR(50),
    question_text TEXT NOT NULL,
    hint TEXT,
    ideal_answer TEXT,
    difficulty INT DEFAULT 3,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Daily Activity table
CREATE TABLE IF NOT EXISTS daily_activity (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    activity_date DATE NOT NULL,
    question_count INT DEFAULT 0,
    total_score INT DEFAULT 0,
    interview_count INT DEFAULT 0,
    UNIQUE(user_id, activity_date)
);

-- Indexes
CREATE INDEX idx_jd_user_id ON job_descriptions(user_id);
CREATE INDEX idx_session_user_id ON interview_sessions(user_id);
CREATE INDEX idx_session_status ON interview_sessions(status);
CREATE INDEX idx_qna_session_id ON interview_qna(session_id);
CREATE INDEX idx_stats_user_id ON user_statistics(user_id);
CREATE INDEX idx_generated_questions_jd_id ON generated_questions(jd_id);

-- [B-2] 복합 인덱스 추가 - Full Table Scan 제거
-- 면접 세션 조회: user_id + started_at DESC 정렬
CREATE INDEX idx_session_user_started ON interview_sessions(user_id, started_at DESC);

-- QnA 조회: session_id + question_order 순서
CREATE INDEX idx_qna_session_order ON interview_qna(session_id, question_order);

-- 통계 조회: user_id + skill_category 복합 인덱스
CREATE INDEX idx_stats_user_category ON user_statistics(user_id, skill_category);

-- 일일 활동: user_id + activity_date DESC 정렬
CREATE INDEX idx_daily_user_date ON daily_activity(user_id, activity_date DESC);

-- 생성 질문: jd_id + question_type 복합 인덱스
CREATE INDEX idx_questions_jd_type ON generated_questions(jd_id, question_type);

-- 면접 QnA 텍스트 검색 (A-1 검색 기능 지원)
CREATE INDEX idx_qna_question_text ON interview_qna USING gin(to_tsvector('simple', question_text));
CREATE INDEX idx_qna_answer_text ON interview_qna USING gin(to_tsvector('simple', answer_text));
