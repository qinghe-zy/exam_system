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
