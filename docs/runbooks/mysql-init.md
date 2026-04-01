# MySQL Initialization Runbook

## Local Target
- host: `127.0.0.1`
- port: `3306`
- database: `exam_system`
- username: `root`
- password: local-only secret from the operator environment

## Import Command Pattern
`mysql --host=127.0.0.1 --port=3306 --user=root --password=*** exam_system < sql/mysql/init.sql`

## Verified Result On 2026-04-01
- imported successfully
- users: 7
- questions: 4
- exam plans: 2
- answer sheets: 1
- anti-cheat events: 1
