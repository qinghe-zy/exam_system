# EVALS

## Validation Matrix
- Structure completeness: required root docs and detailed docs directories created
- Config completeness: `.env.example`, `application.yml`, `application-mysql.yml`, `.gitignore` updated
- Data model consistency: runtime schema synchronized to MySQL init script
- Backend buildability: passed
- Frontend buildability: passed
- Database initialization: passed on local MySQL
- Role and permission chain: validated through student/grader/teacher smoke logins
- Core chain: candidate exam list, workspace load, save, grading task load, analytics load all passed
- Security baseline: no real AI key committed; local DB password only used in runtime validation command

## Commands Run
- `mvn -q -DskipTests compile`
- `mvn -q test`
- `mvn -q -DskipTests package`
- `npm.cmd run build`
- API smoke against local JAR on port `8083`
- MySQL import into local `exam_system`

## Known Gaps
- No browser automation test yet
- No load or concurrency test yet
- No fine-grained permission matrix automation yet
