# UMS — Utilisation Monitoring System

Tracks utilisation of medical equipment (CT / MRI / PET scanners and other modalities — a
dynamic, admin-managed list) across all government health institutions, for Sri Lanka's
Ministry of Health. Institutions report either directly to the line Ministry, or up through
a Provincial Ministry → Provincial Department of Health Services → Regional Department of
Health Services chain.

Sister projects: [`dmis`](../dmis) (document/letter management) and [`fmis`](../fmis) (fuel
management) — this project ports their `Institution`/`Area` hierarchy, `WebUser` RBAC, and
generic `AbstractFacade` CRUD pattern rather than inventing new ones.

## Stack

| Layer | Choice |
|---|---|
| Language / platform | Java 17, Jakarta EE 10 |
| App server | Payara 6, **separate domain** from the Payara 5 domain running dmis/fmis on the same VM |
| Database | MySQL 8, datasource JNDI name `jdbc/ums` |
| ORM | EclipseLink (JPA), persistence unit `umsPU` — schema owned by Flyway, not auto-DDL |
| Server-rendered UI | JSF + PrimeFaces 14 (jakarta classifier), under `/admin/*` |
| Daily data-entry UI | Static PWA (`/entry/*`) + JAX-RS REST API (`/api/*`), same WAR, same session |
| Migrations | Flyway (`src/main/resources/db/migration`) |

## Project layout

```
src/main/java/lk/gov/health/ums/
  entity/     JPA entities (BaseEntity carries the audit/retire fields every entity shares)
  enums/      Static vocabulary: InstitutionType, AreaType, UserRole, MachineStatus
  facade/     AbstractFacade<T> generic CRUD, one thin subclass per entity
  bean/       JSF managed beans (SessionController, *Controller admin screens)
  converter/  JSF @FacesConverter classes for entity-valued <p:selectOneMenu>
  ws/         JAX-RS REST resources backing the PWA + dashboards
  config/     FlywayMigrationListener — runs migrations against jdbc/ums on deploy

src/main/webapp/
  WEB-INF/    web.xml, faces-config.xml, beans.xml, glassfish-web.xml (context-root /ums)
  admin/      JSF/PrimeFaces admin screens — institutions, equipment types, login
  entry/      Daily-entry PWA — index.html, app.js, manifest.json, service-worker.js
  resources/  Static CSS

src/main/resources/
  META-INF/persistence.xml
  db/migration/V1__baseline.sql
```

## Deploying alongside dmis/fmis on the same VM

Install Payara 6 as a **second, independent domain** (own directory, own HTTP/admin ports —
e.g. `--instanceport 8081 --adminport 4849` vs. the existing domain's 8080/4848). Two Payara
processes, no shared state, no risk to the existing dmis/fmis deployment. Add one nginx
location block alongside the existing `/dmis` and `/fmis` ones:

```nginx
location /ums/ {
    proxy_pass http://127.0.0.1:8081/ums/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Configure a `jdbc/ums` JDBC connection pool + resource on the new domain, pointing at a MySQL
schema. On first deploy, `FlywayMigrationListener` runs `V1__baseline.sql` automatically.

**Before going live**: the baseline migration seeds one `admin` account with a placeholder
password hash. Generate a real one (`PasswordUtil.hash("your-password")`) and update that row
before exposing the app.

## Institution registry import

`V2__import_institutions.sql` (1,956 rows: 1,955 from the MoH registry + 1 synthetic grouping
node) is generated from `D:\Dev\his-map\institutions_final.json` by `tools/import-institutions.js`
(`node tools/import-institutions.js`, no dependencies needed). Re-run it if the his-map source
data changes — it overwrites the migration file and self-validates before writing (row counts,
no dangling parent/area references, every `type_atomic` mapped, a few known-record spot checks).
See the comment block at the top of that script for the exact hierarchy-construction rules
(reporting line via `line`/`rdhs`, geography via the standard Sri Lanka district→province
mapping). Known data-quality notes carried over from the source, not import bugs:
- 319 institutions have no registry code (imported with `code = NULL`)
- 6 institution codes are duplicated across two different institutions each in the source
  registry itself — both rows are imported as-is; worth a manual data-quality pass later
- Western Province has no distinct "Provincial Ministry of Health" office in the source
  registry, so its PDHS institution parents directly to the national Ministry

## What's scaffolded vs. what's next

Built: institution hierarchy (now populated from the real MoH registry), equipment-type dynamic
list, equipment registry, the daily status/count entry flow (JSF admin CRUD + REST API + PWA),
RBAC scoping, Flyway baseline + institution import.

Deliberately not yet built (see the architecture decisions log):
- **Procedure/PatientRecord UI** — `Procedure` entity exists; `PatientRecord` (optional
  patient-level detail) is phased in after core rollout, per the confirmed decision.
- **National/provincial/regional dashboards** — `/admin/index.xhtml` is a placeholder;
  the summary REST endpoint and charts aren't built yet.
- **User management screen** — `WebUser` accounts currently need to be created directly
  against the database.
