-- =============================================
-- V28: Moka 人员入离职状态同步表
--   数据来源：t_integration_moka_person（employee_no = persondetail.staffNo，一一对应且唯一）
--   同步目标：Moka ATS 申请状态接口
--             PUT /api-platform/v1/applications/{applicationId}/hired  （IN_SERVICE → 已入职）
--             PUT /api-platform/v1/applications/{applicationId}/resign （QUIT → 已离职）
--   关联机制：application_id（Moka 申请 id）获取机制待定，当前允许为空；
--             处理时 application_id 为空则跳过该记录且不写状态行，
--             机制确定后无需重置历史数据
--   状态口径：hired_sync_status / resign_sync_status 均为 0-PENDING 1-SYNCED 2-SYNC_FAILED
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_moka_person_status (
    -- 内部主键（MyBatis-Plus 雪花生成；employee_no 为业务唯一键，单独建 UNIQUE 约束）
    id                    BIGINT       NOT NULL,
    -- ========== 业务关联字段 ==========
    employee_no           VARCHAR(64)  NOT NULL,  -- 工号（t_integration_moka_person.employee_no = persondetail.staffNo，业务唯一键，UNIQUE）
    user_id               VARCHAR(64),            -- iHR 员工ID（冗余自 t_integration_moka_person.user_id，便于追溯）
    user_name             VARCHAR(128),           -- 姓名（冗余快照，便于排查）
    contact_phone         VARCHAR(32),            -- 手机号（冗余快照，便于排查）
    application_id        INTEGER,                -- Moka 申请 id（获取机制待定，当前为空；为空时跳过同步）
    employee_status       VARCHAR(32),            -- 员工状态快照（IN_SERVICE/QUIT，写入时刷新）
    -- ========== 入职同步状态（hired 接口回写） ==========
    hired_sync_status     SMALLINT     NOT NULL DEFAULT 0,  -- 0-PENDING 1-SYNCED 2-SYNC_FAILED
    hired_error           VARCHAR(500),           -- 最近一次入职同步失败原因摘要
    hired_sync_time       TIMESTAMP,              -- 最近一次入职同步时间
    -- ========== 离职同步状态（resign 接口回写） ==========
    resign_sync_status    SMALLINT     NOT NULL DEFAULT 0,  -- 0-PENDING 1-SYNCED 2-SYNC_FAILED
    resign_error          VARCHAR(500),           -- 最近一次离职同步失败原因摘要
    resign_sync_time      TIMESTAMP,              -- 最近一次离职同步时间
    -- ========== 审计字段 ==========
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted            SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_person_status PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_person_status_employee_no UNIQUE (employee_no)
);

COMMENT ON TABLE  t_integration_moka_person_status IS 'Moka人员入离职状态同步表（t_integration_moka_person → Moka hired/resign 申请状态接口）';

COMMENT ON COLUMN t_integration_moka_person_status.id                 IS '内部主键（雪花生成）';
COMMENT ON COLUMN t_integration_moka_person_status.employee_no       IS '工号（t_integration_moka_person.employee_no = persondetail.staffNo，业务唯一键，UNIQUE）';
COMMENT ON COLUMN t_integration_moka_person_status.user_id           IS 'iHR 员工ID（冗余自 t_integration_moka_person.user_id，便于追溯）';
COMMENT ON COLUMN t_integration_moka_person_status.user_name         IS '姓名（冗余快照，便于排查）';
COMMENT ON COLUMN t_integration_moka_person_status.contact_phone     IS '手机号（冗余快照，便于排查）';
COMMENT ON COLUMN t_integration_moka_person_status.application_id    IS 'Moka 申请 id（获取机制待定，当前为空；为空时跳过同步且不写状态行）';
COMMENT ON COLUMN t_integration_moka_person_status.employee_status   IS '员工状态快照（iHR 原始值 IN_SERVICE/QUIT，写入时刷新）';
COMMENT ON COLUMN t_integration_moka_person_status.hired_sync_status IS '入职同步状态：0-PENDING 1-SYNCED 2-SYNC_FAILED';
COMMENT ON COLUMN t_integration_moka_person_status.hired_error       IS '最近一次入职同步失败原因摘要';
COMMENT ON COLUMN t_integration_moka_person_status.hired_sync_time   IS '最近一次入职同步时间';
COMMENT ON COLUMN t_integration_moka_person_status.resign_sync_status IS '离职同步状态：0-PENDING 1-SYNCED 2-SYNC_FAILED';
COMMENT ON COLUMN t_integration_moka_person_status.resign_error      IS '最近一次离职同步失败原因摘要';
COMMENT ON COLUMN t_integration_moka_person_status.resign_sync_time  IS '最近一次离职同步时间';
COMMENT ON COLUMN t_integration_moka_person_status.created_at        IS '记录创建时间';
COMMENT ON COLUMN t_integration_moka_person_status.updated_at        IS '记录更新时间';
COMMENT ON COLUMN t_integration_moka_person_status.is_deleted        IS '逻辑删除：0-正常 1-删除';

-- 常用查询索引（待处理集合按状态过滤 + LEFT JOIN 走 employee_no 唯一索引）
CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_status_hired
    ON t_integration_moka_person_status(hired_sync_status) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_status_resign
    ON t_integration_moka_person_status(resign_sync_status) WHERE is_deleted = 0;
