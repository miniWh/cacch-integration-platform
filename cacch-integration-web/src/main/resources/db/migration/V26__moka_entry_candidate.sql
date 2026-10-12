-- =============================================
-- V26: Moka 待入职候选人快照表 + iHR 同步状态表（共 5 张表）
--   第一步分三阶段：
--     1a. list_by_condition → 主表 + 3 子表（补全字段为 null）
--     1b. jobs/getJobs → 主表 moka_department_code 回写
--     1c. v3/getOfferExtendInfos → 主表 check_in_date/offer_department_code 回写
--   第二步：本地快照 → iHR entryForms/create（不在本脚本范围）
-- =============================================


-- ---------- 主表：Moka 申请级别字段 + 补全字段 ----------
CREATE TABLE IF NOT EXISTS t_integration_moka_entry_application (
    id                          BIGINT       NOT NULL,
    application_id              INTEGER      NOT NULL,
    candidate_id                INTEGER,
    hire_mode                   INTEGER,
    applied_at                  TIMESTAMP,
    moved_at                    TIMESTAMP,
    moka_created_at             TIMESTAMP,
    moka_updated_at             TIMESTAMP,
    -- Job
    moka_job_id                 VARCHAR(64),
    moka_job_title              VARCHAR(128),
    moka_job_department         VARCHAR(128),
    -- Pipeline
    moka_pipeline_id            INTEGER,
    moka_pipeline_name          VARCHAR(128),
    -- Stage
    moka_stage_id               INTEGER,
    moka_stage_name             VARCHAR(128),
    moka_stage_type             INTEGER,
    -- OwnerInfo
    moka_owner_id               INTEGER,
    moka_owner_name             VARCHAR(64),
    -- ========== 补全字段（Step 1b/1c 回写） ==========
    -- Step 1b：职位详情接口补全
    moka_department_code        VARCHAR(64),
    -- Step 1c：Offer 扩展接口补全
    check_in_date               TIMESTAMP,
    offer_department_code       VARCHAR(64),
    offer_department_name       VARCHAR(128),
    offer_status                VARCHAR(32),
    -- ArchiveReasons（待入职通常为 null）
    archive_reason_id           INTEGER,
    archive_reason_type         INTEGER,
    archive_reason_name         VARCHAR(128),
    archived_at                 TIMESTAMP,
    sync_batch                  VARCHAR(64),
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_application PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_entry_application_appid UNIQUE (application_id)
);

COMMENT ON TABLE  t_integration_moka_entry_application IS 'Moka待入职候选人-申请主表（含补全的部门编码/入职日期）';
COMMENT ON COLUMN t_integration_moka_entry_application.id                   IS '内部主键（雪花生成）';
COMMENT ON COLUMN t_integration_moka_entry_application.application_id      IS 'Moka 申请 ID（业务唯一键，UNIQUE）';
COMMENT ON COLUMN t_integration_moka_entry_application.candidate_id        IS 'Moka 候选人 ID';
COMMENT ON COLUMN t_integration_moka_entry_application.hire_mode          IS '招聘模式：1=社招 2=校招';
COMMENT ON COLUMN t_integration_moka_entry_application.applied_at          IS '申请时间';
COMMENT ON COLUMN t_integration_moka_entry_application.moved_at            IS '移动阶段时间';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_created_at    IS 'Moka 侧创建时间';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_updated_at    IS 'Moka 侧更新时间';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_job_id         IS 'Moka 职位 ID（Step 1b 补全部门编码的查询键）';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_job_title      IS 'Moka 职位名称';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_job_department IS 'Moka 职位原始部门名（list_by_condition 返回）';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_pipeline_id    IS 'Moka 流程 ID';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_stage_id      IS 'Moka 阶段 ID';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_stage_type     IS 'Moka 阶段类型：102=待入职';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_owner_id      IS '申请所有者 ID';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_department_code IS '【Step 1b 补全】职位所属部门编码（code），第二步匹配 iHR 部门的关键字段';
COMMENT ON COLUMN t_integration_moka_entry_application.check_in_date       IS '【Step 1c 补全】入职日期（checkinDate），iHR expectEntryDate 来源';
COMMENT ON COLUMN t_integration_moka_entry_application.offer_department_code IS '【Step 1c 补全】Offer 入职部门编码（备选，通常比职位部门更准确）';
COMMENT ON COLUMN t_integration_moka_entry_application.offer_department_name IS '【Step 1c 补全】Offer 入职部门名称';
COMMENT ON COLUMN t_integration_moka_entry_application.offer_status             IS '【Step 1c 补全】Offer 当前状态：pending/accepted/rejected/invalid/expired（来自 offer 扩展 V3 接口 status 字段）';
COMMENT ON COLUMN t_integration_moka_entry_application.sync_batch          IS '同步批次号（便于追溯）';
COMMENT ON COLUMN t_integration_moka_entry_application.created_at           IS '记录创建时间';
COMMENT ON COLUMN t_integration_moka_entry_application.updated_at           IS '记录更新时间';
COMMENT ON COLUMN t_integration_moka_entry_application.is_deleted            IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_app_cand
    ON t_integration_moka_entry_application(candidate_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_app_job
    ON t_integration_moka_entry_application(moka_job_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_app_dept_code
    ON t_integration_moka_entry_application(moka_department_code) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_app_sync_batch
    ON t_integration_moka_entry_application(sync_batch) WHERE is_deleted = 0;


-- ---------- 子表 1：候选人基础信息 + 联系方式（CandidateBaseInfo + ContactInfo） ----------
CREATE TABLE IF NOT EXISTS t_integration_moka_entry_candidate_base (
    id                          BIGINT       NOT NULL,
    application_id              INTEGER      NOT NULL,
    moka_name                   VARCHAR(64),
    moka_gender                 INTEGER,
    moka_age                    INTEGER,
    moka_birth_date             TIMESTAMP,
    moka_nationality            VARCHAR(64),
    moka_experience             INTEGER,
    moka_candidate_created_at   TIMESTAMP,
    moka_candidate_archived     BOOLEAN,
    moka_candidate_archived_at  TIMESTAMP,
    moka_is_blacklist           BOOLEAN,
    moka_contact_phone          VARCHAR(32),
    moka_contact_email          VARCHAR(128),
    moka_country_calling_code   VARCHAR(16),
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_candidate_base PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_entry_candidate_base_appid UNIQUE (application_id)
);

COMMENT ON TABLE  t_integration_moka_entry_candidate_base IS 'Moka待入职候选人-基础信息（含联系方式）';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.application_id       IS '关联主表 application_id（1:1）';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.moka_name           IS '候选人姓名';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.moka_gender         IS '性别：1=男 2=女';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.moka_contact_phone  IS '手机号（敏感，日志脱敏）';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.moka_contact_email  IS '邮箱';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.is_deleted           IS '逻辑删除：0-正常 1-删除';


-- ---------- 子表 2：毕业院校（GraduateSchool） ----------
CREATE TABLE IF NOT EXISTS t_integration_moka_entry_graduate_school (
    id                          BIGINT       NOT NULL,
    application_id              INTEGER      NOT NULL,
    last_speciality             VARCHAR(128),
    last_school                 VARCHAR(128),
    graduate_date               BIGINT,
    academic_degree             INTEGER,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_graduate_school PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_entry_graduate_school_appid UNIQUE (application_id)
);

COMMENT ON TABLE  t_integration_moka_entry_graduate_school IS 'Moka待入职候选人-毕业院校';
COMMENT ON COLUMN t_integration_moka_entry_graduate_school.application_id IS '关联主表 application_id（1:1）';
COMMENT ON COLUMN t_integration_moka_entry_graduate_school.academic_degree IS '学历：1=其他 2=初中及以下 3=高中 4=中专 5=大专 6=本科 7=硕士 8=MBA 9=博士';
COMMENT ON COLUMN t_integration_moka_entry_graduate_school.is_deleted      IS '逻辑删除：0-正常 1-删除';


-- ---------- 子表 3：证件信息（Citizen） ----------
CREATE TABLE IF NOT EXISTS t_integration_moka_entry_citizen (
    id                          BIGINT       NOT NULL,
    application_id              INTEGER      NOT NULL,
    citizen_id                  VARCHAR(64),
    certificate_type            INTEGER,
    id_card_sign_organization  VARCHAR(128),
    id_card_validity            VARCHAR(128),
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_citizen PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_entry_citizen_appid UNIQUE (application_id)
);

COMMENT ON TABLE  t_integration_moka_entry_citizen IS 'Moka待入职候选人-证件信息';
COMMENT ON COLUMN t_integration_moka_entry_citizen.application_id IS '关联主表 application_id（1:1）';
COMMENT ON COLUMN t_integration_moka_entry_citizen.citizen_id    IS '证件号码（敏感，日志脱敏）';
COMMENT ON COLUMN t_integration_moka_entry_citizen.certificate_type IS '证件类型：1=身份证 2=香港身份证 3=澳门身份证 4=台湾身份证 5=护照 6=其他证件';
COMMENT ON COLUMN t_integration_moka_entry_citizen.is_deleted       IS '逻辑删除：0-正常 1-删除';


-- ---------- 同步状态表 ----------
CREATE TABLE IF NOT EXISTS t_integration_moka_entry_sync (
    id                      BIGINT       NOT NULL,
    application_id          INTEGER      NOT NULL,
    ihr_sync_status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    ihr_entry_id            VARCHAR(64),
    ihr_sync_message        VARCHAR(500),
    ihr_sync_time           TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_sync PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_entry_sync_appid UNIQUE (application_id)
);

COMMENT ON TABLE  t_integration_moka_entry_sync IS 'Moka待入职同步状态表（iHR+未来多系统扩展）';
COMMENT ON COLUMN t_integration_moka_entry_sync.application_id   IS '关联主表 application_id（UNIQUE）';
COMMENT ON COLUMN t_integration_moka_entry_sync.ihr_sync_status  IS 'iHR 同步状态：PENDING=待同步 SUCCESS=成功 FAILED=失败 SKIPPED=跳过';
COMMENT ON COLUMN t_integration_moka_entry_sync.ihr_entry_id     IS 'iHR 返回的 entryId（同步成功时回填）';
COMMENT ON COLUMN t_integration_moka_entry_sync.ihr_sync_message IS '最近一次同步结果/失败原因';
COMMENT ON COLUMN t_integration_moka_entry_sync.is_deleted        IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_sync_status
    ON t_integration_moka_entry_sync(ihr_sync_status) WHERE is_deleted = 0;
