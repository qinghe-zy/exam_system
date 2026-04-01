# DECISIONS

## ADR-0001
Single monolith first. Accepted.

## ADR-0002
Prioritize the real exam chain before advanced extensions. Accepted.

## ADR-0003
Use repository documents as persistent project memory. Accepted.

## ADR-0004
Implement baseline anti-cheat telemetry now and reserve advanced monitoring for later. Accepted.

## ADR-0005
Center the data model around question -> paper -> exam plan -> answer sheet -> grading -> score. Accepted.

## ADR-0006
Use H2 file mode for fast local boot verification while keeping MySQL as the delivery database. Accepted.
Reason: this preserves quick developer feedback without weakening MySQL delivery expectations.

## ADR-0007
Keep paper composition relational through `biz_paper_question` instead of storing hidden JSON-only composition. Accepted.
Reason: relational composition is easier to audit, grade, and analyze.

## ADR-0008
Expose role-filtered menus in the backend rather than hardcoding route visibility in the frontend store. Accepted.
Reason: menu visibility belongs to the security boundary and keeps role separation explainable.
