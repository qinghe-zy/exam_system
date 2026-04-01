# Task Plan

## Goal
Push the current repository from a lightweight exam admin scaffold into a deliverable online examination system with a documented architecture, persistent project memory, verifiable core business flow, and handoff-ready delivery assets.

## Current Status
- Phase: Delivery hardening and remote push preparation
- Overall status: In progress
- Last updated: 2026-04-01

## Phases
| Phase | Status | Objective | Exit Criteria |
|---|---|---|---|
| 0 | completed | Audit repository, code, docs, config, and runtime state | Gaps are recorded in `findings.md` and implementation scope is fixed |
| 1 | completed | Repair and expand persistent docs and project governance files | Root docs, memory files, docs tree, and handoff baseline are coherent |
| 2 | completed | Upgrade backend domain model and SQL to support the core exam chain | Entities, schema, services, and APIs cover exam publication, taking, grading, and analytics |
| 3 | completed | Upgrade frontend flows for admin, candidate, grading, and analysis | Main pages can exercise the new backend chain and remain buildable |
| 4 | completed | Add verification, tests, and runbooks | Build/test results, SQL checks, and usage notes are documented |
| 5 | in_progress | Prepare git delivery state and final handoff | Git repo state, remote target, and manual follow-up are documented |

## Fixed Priority
1. User and permission foundation
2. Question bank
3. Paper assembly
4. Exam plan and publication
5. Candidate exam flow
6. Grading and scoring
7. Result analytics
8. Notice and coordination
9. Admin background capabilities
10. Security, ops, and anti-cheat hardening
11. Extension placeholders

## Constraints
- Stay on the existing stack: Spring Boot 3, Java 17, MyBatis-Plus, Vue 3, TypeScript, Vite, Element Plus, MySQL/H2.
- Do not hardcode production secrets.
- Keep local DB credentials out of committed production config.
- Every meaningful milestone must sync `PLAN.md`, `Documentation.md`, `DECISIONS.md`, `CHANGELOG.md`, and related module docs.
- Treat the current repository as the source of truth; do not rebuild as a blank starter.

## Risks
- Root docs are partially malformed and no longer reflect the implementation accurately.
- The repository is not yet a Git repository, which blocks clean delivery until initialized.
- Current schema only supports shallow CRUD and not the real exam lifecycle.
- Frontend currently focuses on admin CRUD screens and lacks candidate/grading workflows.

## Errors Encountered
| Error | Attempt | Resolution |
|---|---|---|
| `git status` failed because the directory is not a Git repository | 1 | Record as delivery gap and handle during the delivery phase |

## Immediate Next Actions
1. Inspect current Git status and configure the target remote
2. Attempt a push to the requested GitHub repository
3. If push fails, capture the exact blocker in `Documentation.md` and `HANDOFF.md`
