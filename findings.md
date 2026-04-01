# Findings

## Discovery Snapshot
- The repository already contains a runnable backend and frontend, but the business scope is much smaller than the target product scope.
- Backend currently models:
  - `sys_user`, `sys_role`, `sys_menu`
  - `biz_notice`
  - `biz_question_bank`
  - `biz_exam_paper`
  - `biz_exam_record`
- Frontend currently exposes:
  - Login
  - Dashboard
  - User, role, and menu list views
  - Notice list
  - Question bank CRUD
  - Exam paper CRUD
  - Exam record CRUD

## Key Gaps
- No organization, class, or department isolation model exists.
- No exam plan/publication object exists in the actual runtime schema.
- No candidate roster, answer sheet, answer item, grading task, or score summary entities exist.
- No candidate-facing page exists for taking an exam.
- No teacher-facing grading page exists.
- No analytics page exists beyond static dashboard metrics.
- Docs are inconsistent:
  - `README.md` contains broken content and duplicated sections.
  - Several root docs are truncated or only partially filled.
  - Existing `docs/` files are too small for the requested documentation depth.
- Delivery gap:
  - The folder is not a Git repository yet.

## Implementation Direction
- Reuse the current Spring Boot + MyBatis-Plus + Vue stack.
- Extend the schema in-place rather than replacing the project.
- Keep H2 as the default local quick-start profile and support MySQL through profile config and SQL scripts.
- Implement a realistic but compact end-to-end chain:
  - organization and user profile metadata
  - richer question bank
  - paper with explicit paper-question composition
  - exam plan with candidate assignment
  - answer sheet and answer item save/submit
  - objective auto scoring
  - subjective grading workflow
  - score analytics summary
  - audit and anti-cheat event logging basics

## Verification Baseline
- Backend appears to have been built previously because `target/` exists.
- Frontend appears to have been built previously because `dist/` exists.
- No current command output has yet verified the repository after upcoming changes.
