# Project Preferences

## Build & Run
- The user prefers to compile and run the application themselves
- Do not attempt to run maven compile, build, or deploy commands
- If there are errors, the user will report them

## Project Info
- Utilisation Monitoring System (UMS) — tracks medical equipment (CT/MRI/PET/...) utilisation
  across government health institutions for the Ministry of Health, Sri Lanka.
- Jakarta EE 10 / JSF + PrimeFaces 14 (jakarta classifier) / EclipseLink JPA / MySQL / Payara 6.
- Deploys as a separate Payara 6 domain alongside the existing Payara 5 domain that runs
  dmis and fmis on the same VM — see nginx routing note in README.md.
- Schema is owned by Flyway (`src/main/resources/db/migration`), not EclipseLink auto-DDL —
  `persistence.xml` has schema-generation set to "none".
- Sister projects: `D:\Dev\dmis` (letter management) and `D:\Dev\fmis` (fuel management) —
  same author, same architectural lineage. Entity/RBAC/facade patterns were ported from
  those, not invented fresh.
- Full architecture discussion (stack rationale, domain model, decisions log) was produced
  as an Artifact during design — ask the user if they want it re-shared; it's not checked
  into this repo.
