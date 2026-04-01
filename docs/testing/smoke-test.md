# Smoke Test

## Backend
1. `mvn -q -DskipTests compile`
2. `mvn -q test`
3. `mvn -q -DskipTests package`

## Frontend
1. `npm.cmd run build`

## API Smoke
1. Login as `student / student123`
2. Request `/api/exam/candidate/my-exams`
3. Request `/api/exam/candidate/exams/1`
4. Save answers to `/api/exam/candidate/exams/1/save`
5. Login as `grader / grader123`
6. Request `/api/exam/grading/tasks`
7. Login as `teacher / teacher123`
8. Request `/api/exam/analytics/overview`

## Expected Outcome
- All responses should return `code = 0`
- Candidate workspace should return 4 seeded questions
- Grading task list should return the seeded pending task
