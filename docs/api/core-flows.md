# Core API Flows

## Question Bank
- `GET /api/exam/questions`
- `POST /api/exam/questions`
- `PUT /api/exam/questions/{id}`
- `DELETE /api/exam/questions/{id}`

## Paper Studio
- `GET /api/exam/papers`
- `GET /api/exam/papers/{id}`
- `POST /api/exam/papers`
- `PUT /api/exam/papers/{id}`
- `DELETE /api/exam/papers/{id}`

## Exam Release
- `GET /api/exam/plans`
- `POST /api/exam/plans`
- `PUT /api/exam/plans/{id}`
- `DELETE /api/exam/plans/{id}`

## Candidate Flow
- `GET /api/exam/candidate/my-exams`
- `GET /api/exam/candidate/exams/{examPlanId}`
- `POST /api/exam/candidate/exams/{examPlanId}/save`
- `POST /api/exam/candidate/exams/{examPlanId}/submit`
- `POST /api/exam/candidate/exams/{examPlanId}/events`

## Grading And Analysis
- `GET /api/exam/grading/tasks`
- `GET /api/exam/grading/{answerSheetId}`
- `POST /api/exam/grading/{answerSheetId}/submit`
- `GET /api/exam/records`
- `GET /api/exam/analytics/overview`
- `GET /api/exam/proctor/events`
