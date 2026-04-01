# IMPLEMENT

## Delivery Order
1. Keep the system as a single deployable application with explicit module ownership.
2. Extend schema and services before polishing pages.
3. Use runtime verification after each milestone.
4. Sync root docs and detailed docs immediately after milestone completion.

## Current Implementation Notes
- Question, paper, exam plan, candidate, answer sheet, grading, score, and anti-cheat flows are implemented as direct MyBatis-Plus services.
- Paper composition is relational through `biz_paper_question`, not hidden inside a JSON blob.
- Candidate answering writes answer sheets and answer items incrementally.
- Objective scoring happens on submit. Subjective scoring is finalized in the grading center.
- Anti-cheat events are baseline telemetry and not yet a full risk engine.

## Verification Rhythm
- Backend: `mvn -q -DskipTests compile`, `mvn -q test`, `mvn -q -DskipTests package`
- Frontend: `npm.cmd run build`
- API smoke: login + candidate + grading + analytics
- MySQL: recreate and import `sql/mysql/init.sql`, then verify key table counts

## Current Risks
- Frontend bundle size warning remains and should be optimized later.
- Candidate anti-cheat logic is baseline only.
- No automated browser E2E suite exists yet.
