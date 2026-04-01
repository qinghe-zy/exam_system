# Data Model

## Core Tables
- `biz_organization`: school/college/class hierarchy seed
- `sys_role`, `sys_user`, `sys_menu`: auth, identity, role, and visible navigation model
- `biz_question_bank`: item bank with metadata, options, answer, analysis, and default score
- `biz_exam_paper`: paper header
- `biz_paper_question`: explicit paper composition rows
- `biz_exam_plan`: published exam release unit
- `biz_exam_candidate`: candidate assignment rows
- `biz_answer_sheet`: candidate attempt header
- `biz_answer_item`: per-question answer and grading state
- `biz_grading_record`: grading action log
- `biz_score_record`: score publication output
- `biz_anti_cheat_event`: candidate behavior telemetry
- `biz_audit_log`: operational audit trail

## Main Relationships
- `sys_user -> biz_exam_candidate -> biz_answer_sheet -> biz_answer_item -> biz_score_record`
- `biz_question_bank -> biz_paper_question -> biz_exam_paper -> biz_exam_plan`
- `biz_answer_sheet -> biz_grading_record`
- `biz_answer_sheet -> biz_anti_cheat_event`

## Implemented Status Semantics
- Paper publication: `publish_status` draft/published via integer flag
- Exam plan publication: draft/published via integer flag
- Answer sheet: `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `PARTIALLY_GRADED`, `GRADED`
- Score record: `PENDING_GRADING`, `PARTIALLY_GRADED`, `PUBLISHED`

## Authoritative Scripts
- Runtime bootstrap: `backend/src/main/resources/schema.sql` and `backend/src/main/resources/data.sql`
- Delivery bootstrap: `sql/mysql/init.sql`
- Delivery schema baseline: `sql/schema-baseline.sql`
