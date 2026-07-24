CREATE DATABASE IF NOT EXISTS mydatabase
CHARACTER SET 'utf8mb4'
COLLATE 'utf8mb4_unicode_ci';
USE backend_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    provider_type VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(500),
    password VARCHAR(255),
    refresh_token VARCHAR(255),
    is_verified TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME
)CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS study_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    hash_id VARCHAR(100) UNIQUE, -- 스터디 주소
    max_capacity INT NOT NULL, -- 최대 수용 인원
    notice TEXT, -- 스터디 공지사항
    image_url VARCHAR(500),
    created_by INT,
    created_at DATETIME,
    FOREIGN KEY (created_by) REFERENCES users(id)
)CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS schedules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    study_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_datetime DATETIME NOT NULL, -- 시작 날짜 시간
    end_datetime DATETIME NOT NULL, -- 끝 날짜 시간
    memo VARCHAR(500),
    FOREIGN KEY (study_id) REFERENCES study_groups(id)
)CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS study_memberships (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    study_id INT NOT NULL,
    role ENUM('ADMIN','DELEGATE','MEMBER') NOT NULL,
    status ENUM('PENDING','APPROVED','LEFT','REJECTED') NOT NULL,
    joined_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (study_id) REFERENCES study_groups(id)
)CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

-- 카테고리
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    study_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_categories_study
        FOREIGN KEY (study_id) REFERENCES study_groups(id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    -- 같은 스터디 내 동일 이름 금지
    UNIQUE KEY uq_categories_study_name (study_id, name),

    -- 조회 최적화
    KEY idx_categories_study (study_id)
)CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

-- 문서
CREATE TABLE IF NOT EXISTS documents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    study_id INT NOT NULL,
    uploader_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    file_key VARCHAR(1024) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_documents_study (study_id),
    KEY idx_documents_uploader (uploader_id),

    CONSTRAINT fk_documents_study
        FOREIGN KEY (study_id) REFERENCES study_groups(id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT fk_documents_uploader
        FOREIGN KEY (uploader_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
)CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS document_categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    document_id INT NOT NULL,
    category_id INT NOT NULL,

    -- 중복 방지
    UNIQUE KEY uq_doc_cat (document_id, category_id),

    KEY idx_doc (document_id),
    KEY idx_cat (category_id),

    CONSTRAINT fk_doc_cat_document
        FOREIGN KEY (document_id) REFERENCES documents (id)
        ON UPDATE CASCADE ON DELETE CASCADE,

    CONSTRAINT fk_doc_cat_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON UPDATE CASCADE ON DELETE CASCADE
)CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS ai_summaries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    owner_id INT NOT NULL,
    title VARCHAR(255),
    description TEXT,
    model_type VARCHAR(100),
    prompt_type VARCHAR(100),
    summary_json JSON,
    created_at DATETIME,
    FOREIGN KEY (owner_id) REFERENCES users(id)
)CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS ai_summary_documents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    summary_id INT NOT NULL,
    document_id INT NOT NULL,
    FOREIGN KEY (summary_id) REFERENCES ai_summaries(id),
    FOREIGN KEY (document_id) REFERENCES documents(id)
)CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';

CREATE TABLE IF NOT EXISTS study_sessions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    study_group_id INT NOT NULL,
    room_name VARCHAR(128) NOT NULL,
    created_by INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    closed_at DATETIME(6) NULL,
    is_open TINYINT AS (IF(closed_at IS NULL, 1, NULL)) STORED,

    INDEX idx_study_group (study_group_id),
    UNIQUE KEY uq_one_open_per_group (study_group_id, is_open)
    );
