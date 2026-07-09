-- Baseline schema for the Utilisation Monitoring System.
-- Column/table names here must match the @Column/@Table/@JoinColumn
-- annotations in lk.gov.health.ums.entity.* exactly, since
-- persistence.xml sets schema-generation to "none" — Flyway, not
-- EclipseLink, owns this schema (see architecture doc §8).
--
-- PatientRecord (phase 2, optional patient-level detail) is intentionally
-- not part of this baseline — see the confirmed phasing decision.

CREATE TABLE web_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username         VARCHAR(64)  NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    display_name     VARCHAR(150),
    email            VARCHAR(150),
    role             VARCHAR(30)  NOT NULL,
    institution_id   BIGINT,
    created_by       BIGINT,
    created_at       DATETIME,
    last_edit_by     BIGINT,
    last_edit_at     DATETIME,
    retired          BOOLEAN NOT NULL DEFAULT FALSE,
    retired_by       BIGINT,
    retired_at       DATETIME,
    retire_comments  VARCHAR(500),
    CONSTRAINT uq_web_user_username UNIQUE (username),
    CONSTRAINT fk_web_user_created_by   FOREIGN KEY (created_by)   REFERENCES web_user (id),
    CONSTRAINT fk_web_user_last_edit_by FOREIGN KEY (last_edit_by) REFERENCES web_user (id),
    CONSTRAINT fk_web_user_retired_by   FOREIGN KEY (retired_by)   REFERENCES web_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE area (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(30) NOT NULL,
    name            VARCHAR(150) NOT NULL,
    parent_area_id  BIGINT,
    created_by      BIGINT,
    created_at      DATETIME,
    last_edit_by    BIGINT,
    last_edit_at    DATETIME,
    retired         BOOLEAN NOT NULL DEFAULT FALSE,
    retired_by      BIGINT,
    retired_at      DATETIME,
    retire_comments VARCHAR(500),
    CONSTRAINT fk_area_parent_area FOREIGN KEY (parent_area_id) REFERENCES area (id),
    CONSTRAINT fk_area_created_by   FOREIGN KEY (created_by)   REFERENCES web_user (id),
    CONSTRAINT fk_area_last_edit_by FOREIGN KEY (last_edit_by) REFERENCES web_user (id),
    CONSTRAINT fk_area_retired_by   FOREIGN KEY (retired_by)   REFERENCES web_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE institution (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(60) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(30),
    address         VARCHAR(300),
    phone           VARCHAR(150),
    email           VARCHAR(150),
    parent_id       BIGINT,
    area_id         BIGINT,
    created_by      BIGINT,
    created_at      DATETIME,
    last_edit_by    BIGINT,
    last_edit_at    DATETIME,
    retired         BOOLEAN NOT NULL DEFAULT FALSE,
    retired_by      BIGINT,
    retired_at      DATETIME,
    retire_comments VARCHAR(500),
    CONSTRAINT fk_institution_parent     FOREIGN KEY (parent_id)  REFERENCES institution (id),
    CONSTRAINT fk_institution_area       FOREIGN KEY (area_id)    REFERENCES area (id),
    CONSTRAINT fk_institution_created_by   FOREIGN KEY (created_by)   REFERENCES web_user (id),
    CONSTRAINT fk_institution_last_edit_by FOREIGN KEY (last_edit_by) REFERENCES web_user (id),
    CONSTRAINT fk_institution_retired_by   FOREIGN KEY (retired_by)   REFERENCES web_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

ALTER TABLE web_user
    ADD CONSTRAINT fk_web_user_institution FOREIGN KEY (institution_id) REFERENCES institution (id);

CREATE TABLE equipment_type (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    parent_id       BIGINT,
    created_by      BIGINT,
    created_at      DATETIME,
    last_edit_by    BIGINT,
    last_edit_at    DATETIME,
    retired         BOOLEAN NOT NULL DEFAULT FALSE,
    retired_by      BIGINT,
    retired_at      DATETIME,
    retire_comments VARCHAR(500),
    CONSTRAINT fk_equipment_type_parent     FOREIGN KEY (parent_id)     REFERENCES equipment_type (id),
    CONSTRAINT fk_equipment_type_created_by   FOREIGN KEY (created_by)   REFERENCES web_user (id),
    CONSTRAINT fk_equipment_type_last_edit_by FOREIGN KEY (last_edit_by) REFERENCES web_user (id),
    CONSTRAINT fk_equipment_type_retired_by   FOREIGN KEY (retired_by)   REFERENCES web_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE clinical_procedure (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,
    equipment_type_id BIGINT NOT NULL,
    created_by      BIGINT,
    created_at      DATETIME,
    last_edit_by    BIGINT,
    last_edit_at    DATETIME,
    retired         BOOLEAN NOT NULL DEFAULT FALSE,
    retired_by      BIGINT,
    retired_at      DATETIME,
    retire_comments VARCHAR(500),
    CONSTRAINT fk_clinical_procedure_equipment_type FOREIGN KEY (equipment_type_id) REFERENCES equipment_type (id),
    CONSTRAINT fk_clinical_procedure_created_by   FOREIGN KEY (created_by)   REFERENCES web_user (id),
    CONSTRAINT fk_clinical_procedure_last_edit_by FOREIGN KEY (last_edit_by) REFERENCES web_user (id),
    CONSTRAINT fk_clinical_procedure_retired_by   FOREIGN KEY (retired_by)   REFERENCES web_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE equipment (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_id         BIGINT NOT NULL,
    institution_id  BIGINT NOT NULL,
    location        VARCHAR(200),
    asset_tag       VARCHAR(60),
    installed_on    DATE,
    created_by      BIGINT,
    created_at      DATETIME,
    last_edit_by    BIGINT,
    last_edit_at    DATETIME,
    retired         BOOLEAN NOT NULL DEFAULT FALSE,
    retired_by      BIGINT,
    retired_at      DATETIME,
    retire_comments VARCHAR(500),
    CONSTRAINT fk_equipment_type        FOREIGN KEY (type_id)        REFERENCES equipment_type (id),
    CONSTRAINT fk_equipment_institution FOREIGN KEY (institution_id) REFERENCES institution (id),
    CONSTRAINT fk_equipment_created_by   FOREIGN KEY (created_by)   REFERENCES web_user (id),
    CONSTRAINT fk_equipment_last_edit_by FOREIGN KEY (last_edit_by) REFERENCES web_user (id),
    CONSTRAINT fk_equipment_retired_by   FOREIGN KEY (retired_by)   REFERENCES web_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE status_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_id    BIGINT NOT NULL,
    log_date        DATE NOT NULL,
    status          VARCHAR(30) NOT NULL,
    procedure_count INT,
    created_by      BIGINT,
    created_at      DATETIME,
    last_edit_by    BIGINT,
    last_edit_at    DATETIME,
    retired         BOOLEAN NOT NULL DEFAULT FALSE,
    retired_by      BIGINT,
    retired_at      DATETIME,
    retire_comments VARCHAR(500),
    CONSTRAINT uq_status_log_equipment_date UNIQUE (equipment_id, log_date),
    CONSTRAINT fk_status_log_equipment    FOREIGN KEY (equipment_id) REFERENCES equipment (id),
    CONSTRAINT fk_status_log_created_by   FOREIGN KEY (created_by)   REFERENCES web_user (id),
    CONSTRAINT fk_status_log_last_edit_by FOREIGN KEY (last_edit_by) REFERENCES web_user (id),
    CONSTRAINT fk_status_log_retired_by   FOREIGN KEY (retired_by)   REFERENCES web_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_institution_parent ON institution (parent_id);
CREATE INDEX idx_equipment_institution ON equipment (institution_id);
CREATE INDEX idx_status_log_log_date ON status_log (log_date);

-- Seed the first System Admin account. password_hash below is a real Jasypt
-- BasicPasswordEncryptor hash (see bean.PasswordUtil) for a randomly generated
-- initial password -- the plaintext was handed to the requesting user directly
-- and was never written to this repo or any chat/log. Change it on first login
-- (there is no self-service change screen yet -- update password_hash directly,
-- generating a new hash via PasswordUtil.hash()).
INSERT INTO web_user (username, password_hash, display_name, role, retired)
VALUES ('admin', '8FuCw6xcvCD1cukENSbB0Q1EuKq/Dd3m', 'System Administrator', 'SYSTEM_ADMIN', FALSE);
