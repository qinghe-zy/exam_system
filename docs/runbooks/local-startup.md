# Local Startup Runbook

## Backend
1. `cd backend`
2. `mvn -q -DskipTests package`
3. `java -jar target/exam-system-backend-0.1.0-SNAPSHOT.jar`

## Frontend
1. `cd frontend`
2. `npm.cmd install`
3. `npm.cmd run dev`

## Default Ports
- Backend: `8083`
- Frontend: Vite default `5173`

## Local Profiles
- Default backend profile uses H2 file mode for quick validation
- Use MySQL by setting `SPRING_PROFILES_ACTIVE=mysql` plus `MYSQL_*` environment variables
