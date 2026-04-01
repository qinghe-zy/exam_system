# Online Exam System

## Current Stage
The repository is now in the "core exam chain implemented and verified" stage. It remains a clear monolith and has been upgraded from a shallow admin scaffold into a runnable exam platform seed with authoring, exam publication, candidate answering, grading, score output, and anti-cheat event capture.

## Implemented Modules
- User and RBAC baseline: seeded admin, org admin, teacher, grader, proctor, and student accounts with JWT login and menu visibility separation.
- Question bank: richer question metadata, review status, score defaults, knowledge fields, options payload, and answer/analysis storage.
- Paper studio: explicit paper-question composition with per-question score control.
- Exam plan release: bind papers, windows, password, anti-cheat level, and candidate roster.
- Candidate exam flow: assigned exam list, workspace loading, save, submit, and event reporting.
- Grading center: subjective grading workflow with final score publication.
- Score and analytics: score center and exam-level performance metrics.
- Proctor view: baseline anti-cheat event visibility.
- AI placeholder: environment-driven AI gateway configuration without any real key committed.

## Stack
- Backend: Java 17+, Spring Boot 3, MyBatis-Plus, JWT, Knife4j/OpenAPI
- Frontend: Vue 3, TypeScript, Vite, Element Plus, Pinia, Vue Router
- Database: MySQL as the delivery target, H2 file profile for fast local boot validation

## Repository Structure
- `backend/`: Spring Boot application
- `frontend/`: Vue application
- `sql/`: executable schema and seed scripts
- `docs/`: detailed knowledge base by domain
- `database/`, `scripts/`, `tests/`, `infra/`, `monitoring/`: delivery support directories

## Local Accounts
- `admin / admin123`
- `teacher / teacher123`
- `grader / grader123`
- `student / student123`

## Validation Snapshot
- Backend compile: passed
- Backend package: passed
- Backend test context: passed
- Frontend build: passed
- HTTP smoke: student login, candidate exams, candidate workspace, save answers, grader tasks, teacher analytics all passed
- MySQL initialization: passed against local `exam_system`

## Delivery Status
- Git repository: initialized locally
- Remote: target is `https://github.com/qinghe-zy/exam_system.git`
- Push status: pending final remote operation
