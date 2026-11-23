-- V46: Moka 待入职 EHR v2 登记表字段落库（紧急联系人 + 教育经历）
-- 数据库: PostgreSQL 12

ALTER TABLE t_integration_moka_entry_candidate_base
    ADD COLUMN IF NOT EXISTS emergency_contact_name          VARCHAR(64),
    ADD COLUMN IF NOT EXISTS emergency_contact_relation      VARCHAR(32),
    ADD COLUMN IF NOT EXISTS emergency_contact_relation_ihr  VARCHAR(32),
    ADD COLUMN IF NOT EXISTS emergency_contact_phone         VARCHAR(32);

COMMENT ON COLUMN t_integration_moka_entry_candidate_base.emergency_contact_name         IS '紧急联系人姓名';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.emergency_contact_relation     IS '紧急联系人关系';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.emergency_contact_relation_ihr IS '紧急联系人关系';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.emergency_contact_phone        IS '紧急联系人电话';


CREATE TABLE IF NOT EXISTS t_integration_moka_entry_education (
    id                  BIGINT       NOT NULL,
    application_id      INTEGER      NOT NULL,
    moka_education_id   VARCHAR(32),
    education_index     INTEGER      NOT NULL DEFAULT 0,
    school              VARCHAR(256),
    speciality          VARCHAR(256),
    academic_degree     VARCHAR(64),
    start_date          VARCHAR(16),
    end_date            VARCHAR(16),
    is_full_time        VARCHAR(8),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_education PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_moka_entry_education IS 'Moka 待入职候选人-教育经历';
COMMENT ON COLUMN t_integration_moka_entry_education.application_id    IS '关联主表 application_id';
COMMENT ON COLUMN t_integration_moka_entry_education.moka_education_id IS 'Moka educationInfo.id';
COMMENT ON COLUMN t_integration_moka_entry_education.education_index   IS '教育行序（与 customFields.index 对齐）';
COMMENT ON COLUMN t_integration_moka_entry_education.is_full_time      IS '是否全日制：是/否';

CREATE UNIQUE INDEX IF NOT EXISTS uk_t_integration_moka_entry_education_app_edu
    ON t_integration_moka_entry_education (application_id, moka_education_id)
    WHERE is_deleted = 0 AND moka_education_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_t_integration_moka_entry_education_app_idx
    ON t_integration_moka_entry_education (application_id, education_index)
    WHERE is_deleted = 0 AND moka_education_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_education_app
    ON t_integration_moka_entry_education (application_id) WHERE is_deleted = 0;