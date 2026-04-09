DROP TABLE IF EXISTS biz_audit_log;
DROP TABLE IF EXISTS biz_notification_delivery_log;
DROP TABLE IF EXISTS biz_in_app_message;
DROP TABLE IF EXISTS biz_login_risk_log;
DROP TABLE IF EXISTS sys_notification_template;
DROP TABLE IF EXISTS sys_dictionary_item;
DROP TABLE IF EXISTS sys_config_item;
DROP TABLE IF EXISTS biz_anti_cheat_event;
DROP TABLE IF EXISTS biz_grading_record;
DROP TABLE IF EXISTS biz_answer_item;
DROP TABLE IF EXISTS biz_answer_sheet;
DROP TABLE IF EXISTS biz_exam_candidate;
DROP TABLE IF EXISTS biz_exam_plan;
DROP TABLE IF EXISTS biz_paper_question;
DROP TABLE IF EXISTS biz_score_appeal;
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
    session_version INT NOT NULL DEFAULT 0,
    login_fail_count INT NOT NULL DEFAULT 0,
    last_login_failure_at TIMESTAMP NULL,
    lock_until TIMESTAMP NULL,
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
    organization_id BIGINT,
    title VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    content TEXT NOT NULL,
    publish_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE sys_notification_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT,
    template_code VARCHAR(128) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    title_template VARCHAR(255) NOT NULL,
    content_template TEXT NOT NULL,
    status INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE sys_config_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(128) NOT NULL,
    config_name VARCHAR(128) NOT NULL,
    config_group VARCHAR(64) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    description_text VARCHAR(500),
    status INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE sys_dictionary_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_type VARCHAR(64) NOT NULL,
    item_code VARCHAR(128) NOT NULL,
    item_label VARCHAR(128) NOT NULL,
    item_value VARCHAR(255),
    sort_no INT NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
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
    exam_mode VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    batch_label VARCHAR(128),
    exam_room VARCHAR(128),
    source_exam_plan_id BIGINT,
    source_exam_name VARCHAR(128),
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
    sign_in_required INT NOT NULL DEFAULT 0,
    sign_in_start_minutes INT NOT NULL DEFAULT 60,
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
    seat_no VARCHAR(32),
    attempt_count INT NOT NULL DEFAULT 0,
    signed_in_flag INT NOT NULL DEFAULT 0,
    signed_in_at TIMESTAMP NULL,
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
    review_later_flag INT NOT NULL DEFAULT 0,
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
    review_round INT NOT NULL DEFAULT 1,
    grading_action VARCHAR(32) NOT NULL DEFAULT 'INITIAL',
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
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    appeal_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_score_appeal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    score_record_id BIGINT NOT NULL,
    answer_sheet_id BIGINT NOT NULL,
    exam_plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    candidate_name VARCHAR(64) NOT NULL,
    exam_name VARCHAR(128) NOT NULL,
    appeal_reason TEXT NOT NULL,
    expected_outcome VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    resolution_action VARCHAR(32),
    process_comment TEXT,
    processed_by BIGINT,
    processed_by_name VARCHAR(64),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
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
    leave_count INT NOT NULL DEFAULT 1,
    triggered_auto_save INT NOT NULL DEFAULT 0,
    save_version INT NOT NULL DEFAULT 0,
    client_ip VARCHAR(64),
    device_fingerprint VARCHAR(255),
    device_info VARCHAR(1000),
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

CREATE TABLE biz_in_app_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    message_type VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    related_type VARCHAR(64),
    related_id BIGINT,
    read_flag INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_notification_delivery_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT,
    business_type VARCHAR(64) NOT NULL,
    channel_type VARCHAR(32) NOT NULL,
    template_code VARCHAR(128) NOT NULL,
    recipient_user_id BIGINT,
    recipient_name VARCHAR(128),
    recipient_target VARCHAR(128),
    title VARCHAR(255),
    content TEXT NOT NULL,
    related_type VARCHAR(64),
    related_id BIGINT,
    business_key VARCHAR(255) NOT NULL,
    delivery_status VARCHAR(32) NOT NULL,
    provider_trace VARCHAR(255),
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE biz_login_risk_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    user_id BIGINT,
    role_code VARCHAR(64),
    success_flag INT NOT NULL DEFAULT 1,
    client_ip VARCHAR(64),
    user_agent VARCHAR(500),
    device_fingerprint VARCHAR(255),
    device_info VARCHAR(1000),
    risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW',
    risk_reason VARCHAR(500),
    login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
