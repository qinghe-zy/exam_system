# PRD

## Product Goal
Build a high-spec online examination system for schools or institutions on a clear single-application architecture. The product must support the real operational chain from question authoring to score publication, while leaving controlled extension points for future AI, advanced proctoring, and multi-tenant evolution.

## Target Roles
- Super administrator
- Organization administrator
- Teacher / item author
- Grader
- Proctor
- Candidate

## Core Problem
The system must let an institution configure users and roles, manage a structured item bank, assemble exam papers, publish exams to explicit candidate rosters, support timed answering with save/submit behavior, complete subjective grading, publish scores, and review basic anti-cheat events.

## V1 Scope
- JWT login and RBAC navigation separation
- Question bank with metadata and review fields
- Explicit paper-question composition
- Exam plan release with candidate assignment
- Candidate exam workspace with save and submit
- Objective auto-score and subjective grading workflow
- Score center and analytics overview
- Baseline anti-cheat event recording and viewing

## Deferred or Placeholder Scope
- Facial recognition and camera invigilation
- Full AI scoring or recommendation
- Programming-question sandboxing
- Distributed deployment topology
- Multi-tenant SaaS isolation rollout

## Non-Functional Requirements
- Clear module boundaries in one monolith
- Reproducible build and validation flow
- No committed real secrets
- MySQL delivery script consistency
- Auditability of critical actions
- Documentation synchronized with implementation

## Acceptance Intent
The project is only considered complete when the main chain is implemented with code, schema, validation evidence, and handoff documentation rather than only scaffold or diagrams.
