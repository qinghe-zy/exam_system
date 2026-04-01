# Changelog

## [Unreleased]

### Added
- Added core exam domain tables: exam plan, candidate assignment, answer sheet, answer item, grading record, score record, anti-cheat event, and audit log
- Added backend controllers and services for exam release, candidate workflow, grading, analytics, and proctor event viewing
- Added frontend pages for exam release, candidate center, grading center, analytics, and proctor events
- Added AI gateway placeholder configuration and infra package
- Added detailed docs directories and delivery support directories

### Changed
- Upgraded question bank and paper models from lightweight CRUD into richer operational structures
- Reworked score center from manual CRUD into read-only published score records
- Synchronized MySQL init SQL with runtime schema and seed data
- Improved `.gitignore` and `.env.example`

### Verified
- Backend compile, package, and test context
- Frontend production build
- HTTP smoke flow for candidate, grading, and analytics
- Local MySQL initialization import
