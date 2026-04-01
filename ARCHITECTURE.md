# Architecture

## Architecture Strategy
The system intentionally stays as a clear monolith at this stage. Package boundaries are explicit so future extraction remains possible, but current delivery optimizes for understandable end-to-end iteration speed.

## Backend Package Shape
- `controller`: HTTP entry points
- `service`: domain orchestration
- `entity`: persistence entities
- `mapper`: MyBatis-Plus repositories
- `dto`: request contracts
- `vo`: response contracts
- `config`: framework configuration
- `security`: JWT parsing and principal model
- `common`: shared response/entity abstractions
- `infra`: environment-driven external integration placeholders such as AI gateway
- `exception`: business and global exception handling

## Frontend Package Shape
- `api`: backend API wrappers
- `views`: route-level screens
- `components`: reusable UI components
- `stores`: state containers
- `router`: route table and auth guard
- `utils`: storage and helpers
- `hooks`: reusable composition helpers
- `layouts`: shell layout
- `constants`: front-end enums and option lists
- `types`: shared domain types

## Main Runtime Flow
1. Teachers maintain questions and build papers.
2. Teachers publish an exam plan with a paper and candidate roster.
3. Candidates load assigned exams and enter the workspace.
4. Save and submit actions persist answer sheets and answer items.
5. Objective items are auto-scored at submit time.
6. Graders review subjective items and finalize scores.
7. Score center and analytics consume published score records.
8. Candidate workspace telemetry writes anti-cheat events for proctor review.

## Security Model
- Authentication: JWT
- Authorization: role-based route and controller checks
- Role separation: menus filtered by visible role list and controller-level pre-authorization
- Secrets: environment variables only for AI and MySQL-sensitive values

## Persistence Strategy
- H2 file profile remains the default quick-start runtime for local smoke and test context.
- MySQL is the authoritative delivery target and receives synchronized initialization SQL under `sql/mysql/init.sql`.

## Extension Strategy
- AI gateway is pre-wired through `app.ai.*` environment bindings only.
- Advanced proctoring, richer analytics, and deeper collaboration remain additive modules instead of blockers for the current mainline.
