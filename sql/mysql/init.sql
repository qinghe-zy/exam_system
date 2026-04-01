DROP TABLE IF EXISTS biz_audit_log;
DROP TABLE IF EXISTS biz_anti_cheat_event;
DROP TABLE IF EXISTS biz_grading_record;
DROP TABLE IF EXISTS biz_answer_item;
DROP TABLE IF EXISTS biz_answer_sheet;
DROP TABLE IF EXISTS biz_exam_candidate;
DROP TABLE IF EXISTS biz_exam_plan;
DROP TABLE IF EXISTS biz_paper_question;
DROP TABLE IF EXISTS biz_score_record;
DROP TABLE IF EXISTS biz_exam_paper;
DROP TABLE IF EXISTS biz_question_bank;
DROP TABLE IF EXISTS biz_notice;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS biz_organization;

CREATE TABLE biz_organization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_code VARCHAR(64) NOT NULL,
    org_name VARCHAR(128) NOT NULL,
    org_type VARCHAR(32) NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    remark VARCHAR(255),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    full_name VARCHAR(64),
    role_code VARCHAR(64) NOT NULL,
    organization_id BIGINT,
    organization_name VARCHAR(128),
    department_name VARCHAR(128),
    email VARCHAR(128),
    phone VARCHAR(32),
    candidate_no VARCHAR(64),
    status INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    path VARCHAR(128) NOT NULL,
    component VARCHAR(128),
    icon VARCHAR(64),
    permission_code VARCHAR(128),
    visible_roles VARCHAR(255),
    parent_id BIGINT NOT NULL DEFAULT 0,
    sort_no INT NOT NULL DEFAULT 0,
    menu_type VARCHAR(32) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    content TEXT NOT NULL,
    publish_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_question_bank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_code VARCHAR(64) NOT NULL,
    organization_id BIGINT,
    subject VARCHAR(64) NOT NULL,
    question_type VARCHAR(32) NOT NULL,
    difficulty_level VARCHAR(32) NOT NULL,
    stem TEXT NOT NULL,
    options_json TEXT,
    answer_key TEXT NOT NULL,
    analysis_text TEXT,
    knowledge_point VARCHAR(128),
    chapter_name VARCHAR(128),
    source_name VARCHAR(128),
    tags VARCHAR(255),
    default_score DECIMAL(10,2) NOT NULL DEFAULT 10,
    reviewer_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    version_no INT NOT NULL DEFAULT 1,
    status INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_exam_paper (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paper_code VARCHAR(64) NOT NULL,
    organization_id BIGINT,
    paper_name VARCHAR(128) NOT NULL,
    subject VARCHAR(64) NOT NULL,
    assembly_mode VARCHAR(32) NOT NULL,
    description_text VARCHAR(500),
    duration_minutes INT NOT NULL,
    total_score DECIMAL(10,2) NOT NULL,
    pass_score DECIMAL(10,2) NOT NULL,
    question_count INT NOT NULL DEFAULT 0,
    publish_status INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_paper_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paper_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    sort_no INT NOT NULL,
    score DECIMAL(10,2) NOT NULL,
    required_flag INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_exam_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_code VARCHAR(64) NOT NULL,
    exam_name VARCHAR(128) NOT NULL,
    organization_id BIGINT,
    paper_id BIGINT NOT NULL,
    paper_name VARCHAR(128) NOT NULL,
    subject VARCHAR(64) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration_minutes INT NOT NULL,
    pass_score DECIMAL(10,2) NOT NULL,
    candidate_scope VARCHAR(64) NOT NULL DEFAULT 'ASSIGNED',
    attempt_limit INT NOT NULL DEFAULT 1,
    exam_password VARCHAR(64),
    late_entry_minutes INT NOT NULL DEFAULT 15,
    early_submit_minutes INT NOT NULL DEFAULT 0,
    auto_submit_enabled INT NOT NULL DEFAULT 1,
    anti_cheat_level VARCHAR(32) NOT NULL DEFAULT 'BASIC',
    instruction_text TEXT,
    status INT NOT NULL DEFAULT 1,
    publish_status INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_exam_candidate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    candidate_name VARCHAR(64) NOT NULL,
    organization_name VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED',
    access_code VARCHAR(32),
    attempt_count INT NOT NULL DEFAULT 0,
    assigned_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_answer_sheet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_plan_id BIGINT NOT NULL,
    paper_id BIGINT NOT NULL,
    paper_name VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    candidate_name VARCHAR(64) NOT NULL,
    started_at TIMESTAMP,
    submitted_at TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    objective_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    subjective_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    final_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    auto_submit_flag INT NOT NULL DEFAULT 0,
    save_version INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_answer_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_sheet_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_type VARCHAR(32) NOT NULL,
    question_order INT NOT NULL,
    answer_content TEXT,
    max_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    score_awarded DECIMAL(10,2) NOT NULL DEFAULT 0,
    auto_scored INT NOT NULL DEFAULT 0,
    marked_flag INT NOT NULL DEFAULT 0,
    review_comment TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_grading_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_sheet_id BIGINT NOT NULL,
    answer_item_id BIGINT NOT NULL,
    grader_id BIGINT NOT NULL,
    grader_name VARCHAR(64) NOT NULL,
    score_awarded DECIMAL(10,2) NOT NULL DEFAULT 0,
    comment_text TEXT,
    graded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'FINALIZED',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_score_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_plan_id BIGINT NOT NULL,
    answer_sheet_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    candidate_name VARCHAR(64) NOT NULL,
    exam_name VARCHAR(128) NOT NULL,
    paper_name VARCHAR(128) NOT NULL,
    submitted_at TIMESTAMP,
    objective_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    subjective_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    final_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    passed_flag INT NOT NULL DEFAULT 0,
    published_flag INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_anti_cheat_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_plan_id BIGINT NOT NULL,
    answer_sheet_id BIGINT,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    detail_text VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT,
    operator_name VARCHAR(64),
    module_name VARCHAR(64) NOT NULL,
    action_name VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id BIGINT,
    detail_text VARCHAR(1000),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
INSERT INTO biz_organization (id, org_code, org_name, org_type, parent_id, status, deleted) VALUES
    (1, 'QHU', 'Qinghe University', 'SCHOOL', 0, 1, 0),
    (2, 'QHU-CS', 'School of Computer Science', 'COLLEGE', 1, 1, 0),
    (3, 'QHU-CS-2401', 'Software Engineering Class 2401', 'CLASS', 2, 1, 0);

INSERT INTO sys_role (id, role_code, role_name, remark, deleted) VALUES
    (1, 'ADMIN', 'Super Administrator', 'Platform-wide management role', 0),
    (2, 'ORG_ADMIN', 'Organization Administrator', 'School or institution operations role', 0),
    (3, 'TEACHER', 'Teacher / Item Author', 'Question and exam authoring role', 0),
    (4, 'GRADER', 'Grader', 'Subjective grading role', 0),
    (5, 'PROCTOR', 'Proctor', 'Exam monitoring role', 0),
    (6, 'STUDENT', 'Candidate', 'Exam participant role', 0);

INSERT INTO sys_user (id, username, password, nickname, full_name, role_code, organization_id, organization_name, department_name, email, phone, candidate_no, status, deleted) VALUES
    (1, 'admin', '{noop}admin123', 'Platform Admin', 'Platform Admin', 'ADMIN', 1, 'Qinghe University', 'Platform Center', 'admin@example.local', '13800000001', NULL, 1, 0),
    (2, 'orgadmin', '{noop}orgadmin123', 'School Admin', 'School Admin', 'ORG_ADMIN', 2, 'School of Computer Science', 'Academic Office', 'orgadmin@example.local', '13800000002', NULL, 1, 0),
    (3, 'teacher', '{noop}teacher123', 'Exam Teacher', 'Liu Teacher', 'TEACHER', 2, 'School of Computer Science', 'Software Engineering', 'teacher@example.local', '13800000003', NULL, 1, 0),
    (4, 'grader', '{noop}grader123', 'Grading Lead', 'Chen Grader', 'GRADER', 2, 'School of Computer Science', 'Teaching Office', 'grader@example.local', '13800000004', NULL, 1, 0),
    (5, 'proctor', '{noop}proctor123', 'Exam Proctor', 'Wu Proctor', 'PROCTOR', 2, 'School of Computer Science', 'Academic Office', 'proctor@example.local', '13800000005', NULL, 1, 0),
    (6, 'student', '{noop}student123', 'Student One', 'Zhang San', 'STUDENT', 3, 'Software Engineering Class 2401', 'Class 2401', 'student@example.local', '13800000006', '20240001', 1, 0),
    (7, 'student2', '{noop}student123', 'Student Two', 'Li Si', 'STUDENT', 3, 'Software Engineering Class 2401', 'Class 2401', 'student2@example.local', '13800000007', '20240002', 1, 0);

INSERT INTO sys_menu (id, name, path, component, icon, permission_code, visible_roles, parent_id, sort_no, menu_type, deleted) VALUES
    (1, 'Dashboard', '/dashboard', 'dashboard/DashboardView', 'Odometer', 'dashboard:view', 'ADMIN,ORG_ADMIN,TEACHER,GRADER,PROCTOR,STUDENT', 0, 1, 'MENU', 0),
    (2, 'System Management', '/system', '', 'Setting', 'system:view', 'ADMIN,ORG_ADMIN', 0, 2, 'MENU', 0),
    (3, 'Users', '/system/users', 'system/UserView', 'User', 'sys:user:view', 'ADMIN,ORG_ADMIN', 2, 1, 'PAGE', 0),
    (4, 'Roles', '/system/roles', 'system/RoleView', 'Collection', 'sys:role:view', 'ADMIN', 2, 2, 'PAGE', 0),
    (5, 'Menus', '/system/menus', 'system/MenuView', 'Menu', 'sys:menu:view', 'ADMIN', 2, 3, 'PAGE', 0),
    (6, 'Exam Notices', '/notices', 'notices/NoticeView', 'Bell', 'biz:notice:view', 'ADMIN,ORG_ADMIN,TEACHER,GRADER,PROCTOR,STUDENT', 0, 3, 'MENU', 0),
    (7, 'Exam Operations', '/exam', '', 'Reading', 'exam:view', 'ADMIN,ORG_ADMIN,TEACHER,GRADER,PROCTOR', 0, 4, 'MENU', 0),
    (8, 'Question Bank', '/exam/questions', 'exam/QuestionBankView', 'Document', 'exam:question:view', 'ADMIN,ORG_ADMIN,TEACHER', 7, 1, 'PAGE', 0),
    (9, 'Paper Studio', '/exam/papers', 'exam/ExamPaperView', 'Collection', 'exam:paper:view', 'ADMIN,ORG_ADMIN,TEACHER', 7, 2, 'PAGE', 0),
    (10, 'Exam Plans', '/exam/plans', 'exam/ExamPlanView', 'Calendar', 'exam:plan:view', 'ADMIN,ORG_ADMIN,TEACHER', 7, 3, 'PAGE', 0),
    (11, 'Grading Center', '/exam/grading', 'exam/GradingView', 'EditPen', 'exam:grading:view', 'ADMIN,ORG_ADMIN,GRADER,TEACHER', 7, 4, 'PAGE', 0),
    (12, 'Score Center', '/exam/records', 'exam/ExamRecordView', 'Histogram', 'exam:record:view', 'ADMIN,ORG_ADMIN,TEACHER,GRADER', 7, 5, 'PAGE', 0),
    (13, 'Analytics', '/exam/analytics', 'exam/AnalysisView', 'DataLine', 'exam:analytics:view', 'ADMIN,ORG_ADMIN,TEACHER', 7, 6, 'PAGE', 0),
    (14, 'Candidate Center', '/candidate', '', 'Tickets', 'candidate:view', 'STUDENT', 0, 5, 'MENU', 0),
    (15, 'My Exams', '/candidate/exams', 'exam/CandidateExamView', 'Tickets', 'candidate:exam:view', 'STUDENT', 14, 1, 'PAGE', 0),
    (16, 'Proctor Events', '/exam/proctor', 'exam/ProctorView', 'Warning', 'exam:proctor:view', 'ADMIN,ORG_ADMIN,PROCTOR', 7, 7, 'PAGE', 0);

INSERT INTO biz_notice (id, title, category, status, content, deleted) VALUES
    (1, 'Midterm window published', 'exam', 1, 'The Spring term midterm examination window has been published for Software Engineering Class 2401.', 0),
    (2, 'Grading handoff ready', 'grading', 1, 'Subjective grading tasks are assigned to the grading team after objective scoring completes.', 0);

INSERT INTO biz_question_bank (id, question_code, organization_id, subject, question_type, difficulty_level, stem, options_json, answer_key, analysis_text, knowledge_point, chapter_name, source_name, tags, default_score, reviewer_status, version_no, status, deleted) VALUES
    (1, 'Q-JAVA-001', 2, 'Java Web', 'SINGLE_CHOICE', 'MEDIUM', 'Which annotation marks a Spring MVC controller class?', '[""@Controller"", ""@Service"", ""@Component"", ""@Mapper""]', '@Controller', 'Spring MVC request entry is typically a controller class annotated with @Controller or @RestController.', 'Spring MVC', 'Web Layer', 'Internal Bank', 'spring,mvc,controller', 20, 'APPROVED', 1, 1, 0),
    (2, 'Q-DB-001', 2, 'Database', 'MULTIPLE_CHOICE', 'MEDIUM', 'Which items are ACID properties of a transaction?', '[""Atomicity"", ""Consistency"", ""Isolation"", ""Durability"", ""Compression""]', 'Atomicity|Consistency|Isolation|Durability', 'ACID is the standard transaction integrity model.', 'Transactions', 'Core Concepts', 'Internal Bank', 'acid,transaction', 20, 'APPROVED', 1, 1, 0),
    (3, 'Q-SE-001', 2, 'Software Engineering', 'TRUE_FALSE', 'EASY', 'Unit tests should focus on the behavior of the smallest practical unit.', '[""True"", ""False""]', 'True', 'Unit testing is designed around isolated units with controlled dependencies.', 'Testing', 'Quality Assurance', 'Internal Bank', 'testing,unit-test', 10, 'APPROVED', 1, 1, 0),
    (4, 'Q-DESIGN-001', 2, 'Architecture', 'SHORT_ANSWER', 'HARD', 'Explain why clear module boundaries help a single-application exam platform evolve safely.', NULL, 'Reference answer', 'The answer should mention maintainability, isolation of change, explicit responsibility, and easier future extraction.', 'Architecture', 'Modular Design', 'Teaching Team', 'architecture,module-boundary', 50, 'APPROVED', 1, 1, 0);

INSERT INTO biz_exam_paper (id, paper_code, organization_id, paper_name, subject, assembly_mode, description_text, duration_minutes, total_score, pass_score, question_count, publish_status, deleted) VALUES
    (1, 'PAPER-2026-001', 2, 'Spring Core Midterm Paper', 'Software Engineering', 'MANUAL', 'A mixed paper covering web, database, testing, and architecture topics.', 90, 100, 60, 4, 1, 0);

INSERT INTO biz_paper_question (id, paper_id, question_id, sort_no, score, required_flag, deleted) VALUES
    (1, 1, 1, 1, 20, 1, 0),
    (2, 1, 2, 2, 20, 1, 0),
    (3, 1, 3, 3, 10, 1, 0),
    (4, 1, 4, 4, 50, 1, 0);

INSERT INTO biz_exam_plan (id, exam_code, exam_name, organization_id, paper_id, paper_name, subject, start_time, end_time, duration_minutes, pass_score, candidate_scope, attempt_limit, exam_password, late_entry_minutes, early_submit_minutes, auto_submit_enabled, anti_cheat_level, instruction_text, status, publish_status, deleted) VALUES
    (1, 'EXAM-2026-001', 'Spring Midterm Live Exam', 2, 1, 'Spring Core Midterm Paper', 'Software Engineering', TIMESTAMP '2026-04-01 09:00:00', TIMESTAMP '2026-04-30 23:00:00', 90, 60, 'ASSIGNED', 1, 'MIDTERM2026', 15, 0, 1, 'BASIC', 'Please keep fullscreen enabled, avoid tab switching, and submit before the deadline.', 1, 1, 0),
    (2, 'EXAM-2026-002', 'Architecture Drill', 2, 1, 'Spring Core Midterm Paper', 'Software Engineering', TIMESTAMP '2026-05-10 09:00:00', TIMESTAMP '2026-05-12 18:00:00', 60, 60, 'ASSIGNED', 1, NULL, 10, 0, 1, 'BASIC', 'Draft training exam for architecture review.', 1, 0, 0);

INSERT INTO biz_exam_candidate (id, exam_plan_id, user_id, candidate_name, organization_name, status, access_code, attempt_count, deleted) VALUES
    (1, 1, 6, 'Zhang San', 'Software Engineering Class 2401', 'ASSIGNED', 'A001', 0, 0),
    (2, 1, 7, 'Li Si', 'Software Engineering Class 2401', 'SUBMITTED', 'A002', 1, 0);

INSERT INTO biz_answer_sheet (id, exam_plan_id, paper_id, paper_name, user_id, candidate_name, started_at, submitted_at, status, objective_score, subjective_score, final_score, auto_submit_flag, save_version, deleted) VALUES
    (1, 1, 1, 'Spring Core Midterm Paper', 7, 'Li Si', TIMESTAMP '2026-04-01 10:00:00', TIMESTAMP '2026-04-01 10:40:00', 'SUBMITTED', 50, 0, 50, 0, 4, 0);

INSERT INTO biz_answer_item (id, answer_sheet_id, question_id, question_type, question_order, answer_content, max_score, score_awarded, auto_scored, marked_flag, review_comment, status, deleted) VALUES
    (1, 1, 1, 'SINGLE_CHOICE', 1, '@Controller', 20, 20, 1, 1, 'Auto scored', 'AUTO_SCORED', 0),
    (2, 1, 2, 'MULTIPLE_CHOICE', 2, 'Atomicity|Consistency|Isolation|Durability', 20, 20, 1, 1, 'Auto scored', 'AUTO_SCORED', 0),
    (3, 1, 3, 'TRUE_FALSE', 3, 'True', 10, 10, 1, 1, 'Auto scored', 'AUTO_SCORED', 0),
    (4, 1, 4, 'SHORT_ANSWER', 4, 'Clear module boundaries reduce change impact, isolate responsibilities, and preserve maintainability while future extraction remains possible.', 50, 0, 0, 0, NULL, 'PENDING_GRADING', 0);

INSERT INTO biz_score_record (id, exam_plan_id, answer_sheet_id, user_id, candidate_name, exam_name, paper_name, submitted_at, objective_score, subjective_score, final_score, passed_flag, published_flag, status, deleted) VALUES
    (1, 1, 1, 7, 'Li Si', 'Spring Midterm Live Exam', 'Spring Core Midterm Paper', TIMESTAMP '2026-04-01 10:40:00', 50, 0, 50, 0, 0, 'PENDING_GRADING', 0);

INSERT INTO biz_anti_cheat_event (id, exam_plan_id, answer_sheet_id, user_id, event_type, severity, detail_text, occurred_at, deleted) VALUES
    (1, 1, 1, 7, 'TAB_SWITCH', 'MEDIUM', 'Candidate switched away from the exam page once during the session.', TIMESTAMP '2026-04-01 10:21:00', 0);

INSERT INTO biz_audit_log (id, operator_id, operator_name, module_name, action_name, target_type, target_id, detail_text, deleted) VALUES
    (1, 3, 'Liu Teacher', 'QUESTION_BANK', 'CREATE', 'QUESTION', 4, 'Added architecture short-answer question to the spring midterm pool.', 0),
    (2, 3, 'Liu Teacher', 'EXAM_PLAN', 'PUBLISH', 'EXAM_PLAN', 1, 'Published Spring Midterm Live Exam for Software Engineering Class 2401.', 0);
