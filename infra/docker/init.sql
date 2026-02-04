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
    answered_at TIMESTAMP
);

-- User Statistics table
CREATE TABLE IF NOT EXISTS user_statistics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    skill_category VARCHAR(50) NOT NULL,
    total_questions INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    correct_rate DECIMAL(5,2) DEFAULT 0,
    weak_points JSONB DEFAULT '[]',
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, skill_category)
);

-- Indexes
CREATE INDEX idx_jd_user_id ON job_descriptions(user_id);
CREATE INDEX idx_session_user_id ON interview_sessions(user_id);
CREATE INDEX idx_session_status ON interview_sessions(status);
CREATE INDEX idx_qna_session_id ON interview_qna(session_id);
CREATE INDEX idx_stats_user_id ON user_statistics(user_id);
