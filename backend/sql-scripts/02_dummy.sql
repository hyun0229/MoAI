-- Dummy Data for MySQL Tables

-- 1. Users Table
INSERT INTO users (id, name, email, password, provider_type, profile_image_url, created_at, refresh_token) VALUES
(1, 'Alice Smith', 'alice.smith@example.com', '$2a$12$dsCAfIheorNo.7hmfx.4B.1jjAZ8HTYUMk7vyJf7zmONSyMZOvV.S', 'email', 'https://placehold.co/100x100/FF5733/FFFFFF?text=AS', NOW(), 'refresh_token_alice'), -- orginal_password : hased_pass_alice
(2, 'Bob Johnson', 'bob.johnson@example.com', '$2a$12$voS7nCUTyrDQW6nBFeYD5.xk3c3N/gIRVNPMNPwvoJ8E4zoLHXgN6', 'google', 'https://placehold.co/100x100/33FF57/000000?text=BJ', NOW(), 'refresh_token_bob'), -- original password : hashed_pass_bob
(3, 'Charlie Brown', 'charlie.brown@example.com', '$2a$12$orbtHx6TIlKn8pkvMLnfvOWpk7Sw4tbdjcHjLa1Km0fbv3VllgL/a', 'email', 'https://placehold.co/100x100/3357FF/FFFFFF?text=CB', NOW(), 'refresh_token_charlie'); -- original password : hashed_pass_charlie

-- 2. Study Groups Table
INSERT INTO study_groups (id, name, description, image_url, created_by, created_at, invite_url) VALUES
(101, 'Data Science Enthusiasts', 'A group for discussing and learning data science concepts.', 'https://placehold.co/200x150/FFC300/000000?text=DS', 1, NOW(), 'https://example.com/invite/data-science'),
(102, 'Web Dev Masters', 'Mastering front-end and back-end web development.', 'https://placehold.co/200x150/DAF7A6/000000?text=WD', 2, NOW(), 'https://example.com/invite/web-dev'),
(103, 'AI Research Group', 'Exploring the latest in Artificial Intelligence research.', 'https://placehold.co/200x150/C70039/FFFFFF?text=AI', 1, NOW(), 'https://example.com/invite/ai-research');

-- 3. Categories Table
-- Assuming global categories are associated with the first study group for simplicity
INSERT INTO categories (id, study_id, name) VALUES
(201, 101, 'Programming'),
(202, 101, 'Mathematics'),
(203, 101, 'History'),
(204, 101, 'Machine Learning'), -- Was for study 301 -> 101
(205, 102, 'Frontend Frameworks'); -- Was for study 302 -> 102

-- 4. Schedules Table
INSERT INTO schedules (id, study_id, title, datetime, memo, is_meeting_reserved) VALUES
(401, 101, 'Python Basics Session', '2025-08-01 10:00:00', 'Covering variables and data types.', TRUE),
(402, 102, 'React Components Workshop', '2025-08-05 14:30:00', 'Hands-on session for component creation.', FALSE),
(403, 101, 'Data Structures in Python', '2025-08-08 11:00:00', 'Reviewing lists, dictionaries, and tuples.', TRUE);

-- 5. Study Memberships Table
INSERT INTO study_memberships (id, user_id, study_id, role, status, joined_at) VALUES
(501, 1, 101, '관리자', '승인', NOW()),
(502, 2, 101, '일반', '승인', NOW()),
(503, 3, 101, '일반', '대기중', NOW()),
(504, 1, 102, '일반', '승인', NOW()),
(505, 2, 103, '관리자', '승인', NOW());

-- 6. Documents Table
INSERT INTO documents (id, study_id, category_id, uploader_id, title, file_path, update_date) VALUES
(601, 101, 201, 1, 'Python Cheatsheet', '/docs/python_cheatsheet.pdf', NOW()),
(602, 102, 205, 2, 'React Component Lifecycle', '/docs/react_lifecycle.pptx', NOW()),
(603, 101, 204, 1, 'Linear Regression Notes', '/docs/linear_regression.docx', NOW()),
(604, 101, 202, 3, 'Calculus Review', '/docs/calculus_review.pdf', NOW()); -- Was not tied to a study, associating with 101

-- 7. AI Summaries Table
INSERT INTO ai_summaries (id, owner_id, title, description, model_type, prompt_type, file_path, created_at) VALUES
(701, 1, 'Python Cheatsheet Summary', 'Summary of Python Cheatsheet focusing on data types.', 'gemini-pro', 'summarization', '/summaries/python_cheatsheet_summary.txt', NOW()),
(702, 2, 'React Lifecycle Summary', 'Key points from React Component Lifecycle presentation.', 'claude-3', 'extraction', '/summaries/react_lifecycle_summary.txt', NOW());

-- 8. AI Summary Documents Table (linking AI summaries to original documents)
INSERT INTO ai_summary_documents (id, summary_id, document_id) VALUES
(801, 701, 601),
(802, 702, 602);
