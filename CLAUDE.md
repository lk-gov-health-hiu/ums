# Project Preferences

## Build & Run
- By default, the user prefers to compile and run the application themselves —
  do not attempt maven compile/build/deploy commands unprompted.
- Exception: if the user explicitly says to compile/build/deploy in the moment (e.g. "compile
  this", "ok, deploy"), go ahead for that request. This is per-instance authorization, not a
  standing green light — still don't do it unprompted afterwards.
- If there are errors, report them back rather than guessing at environment fixes (JDK
  switches, installing tooling, editing PATH/JAVA_HOME) — the user's own build setup may
  differ from what's on this machine.

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
