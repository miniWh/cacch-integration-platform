-- =============================================
-- V24: Moka 人员信息中间表
--   数据来源：PG 库 persondetail（iHR 同步入口）
--   关联表：  t_integration_ihr_department（
--             persondetail.departmentId = ihr_dept_id，
--             取 department_code 作为 Moka API 入参）
--   同步目标：Moka 开放平台 POST /api-platform/v1/users/syncInfo
--   roleId：  阶段一固定 DEFAULT 223379（Moka 默认角色 ID）；
--             阶段二拉取角色后按 jobTitle 匹配 t_integration_moka_role
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_moka_person (
    -- 内部主键（MyBatis-Plus 雪花生成；user_id 为业务主键，单独建 UNIQUE 约束）
    id                    BIGINT       NOT NULL,
    -- ========== iHR 业务字段 ==========
    user_id               VARCHAR(64)  NOT NULL,   -- iHR 员工ID（persondetail.userId，业务唯一键，UNIQUE）
    employee_no           VARCHAR(64),              -- 工号（Moka API number）
    user_name             VARCHAR(128),             -- 姓名（Moka API name）
    nickname              VARCHAR(128),             -- 昵称/花名（Moka API nickname，暂与 user_name 同值）
    company_email         VARCHAR(200),             -- 工作邮箱（Moka API email，允许为空）
    contact_phone         VARCHAR(32),              -- 工作电话（Moka API phone）
    -- ========== Moka 推送参数（接口 1 落库时直接写好，接口 2 原样推给 Moka） ==========
    role_id               INTEGER      NOT NULL DEFAULT 223379, -- Moka 自定义角色 ID（Moka API roleId）。
                                                           -- 阶段一固定值 223379（Moka 默认角色）；
                                                           -- 阶段二从 t_integration_moka_role 按 jobTitle 匹配后更新
    department_code       VARCHAR(64),              -- 部门编号（接口 1 通过 persondetail.departmentId
                                                    -- 关联 t_integration_ihr_department.ihr_dept_id 得到）
    superior_email        VARCHAR(200),             -- 直属领导邮箱（从 persondetail.superiorsInfo 提取，
                                                    -- 映射 Moka API superiorEmail）
    employee_status       VARCHAR(32),              -- 员工状态（iHR 原始值）
    deactivated           SMALLINT NOT NULL DEFAULT 0,  -- Moka API deactivated：0-不禁用 1-禁用
                                                       -- 注意：首次创建用户传 1 则不会创建成功
    locale                VARCHAR(16) NOT NULL DEFAULT 'zh-CN',   -- Moka 用户语言
    timezone              VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai', -- Moka 用户时区
    -- ========== Moka 推送控制字段 ==========
    moka_sync_status      SMALLINT NOT NULL DEFAULT 0,  -- 0-PENDING 1-SYNCED 2-SYNC_FAILED
    last_sync_time        TIMESTAMP,                    -- 最近一次推送 Moka 的时间
    last_sync_result      VARCHAR(500),                 -- 最近一次推送结果摘要
    -- ========== 审计字段 ==========
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted            SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_person PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_person_user_id UNIQUE (user_id)
);

COMMENT ON TABLE  t_integration_moka_person IS 'Moka人员信息中间表（由 persondetail + t_integration_ihr_department 关联同步）';

COMMENT ON COLUMN t_integration_moka_person.id                  IS '内部主键（雪花生成）';
COMMENT ON COLUMN t_integration_moka_person.user_id            IS 'iHR 员工ID（persondetail.userId，业务唯一键，UNIQUE）';
COMMENT ON COLUMN t_integration_moka_person.employee_no       IS '工号（Moka API number）';
COMMENT ON COLUMN t_integration_moka_person.user_name          IS '姓名（Moka API name）';
COMMENT ON COLUMN t_integration_moka_person.nickname           IS '昵称/花名（Moka API nickname，暂与 user_name 同值）';
COMMENT ON COLUMN t_integration_moka_person.company_email      IS '工作邮箱（Moka API email，允许为空）';
COMMENT ON COLUMN t_integration_moka_person.contact_phone      IS '工作电话（Moka API phone）';
COMMENT ON COLUMN t_integration_moka_person.role_id            IS 'Moka 自定义角色 ID（Moka API roleId）。阶段一默认固定值 223379；阶段二按 jobTitle 匹配 t_integration_moka_role 后更新';
COMMENT ON COLUMN t_integration_moka_person.department_code   IS '部门编号（接口 1 关联 t_integration_ihr_department.ihr_dept_id 得到 department_code）';
COMMENT ON COLUMN t_integration_moka_person.superior_email     IS '直属领导邮箱（从 persondetail.superiorsInfo 提取，映射 Moka API superiorEmail）';
COMMENT ON COLUMN t_integration_moka_person.employee_status    IS '员工状态（iHR 原始值）';
COMMENT ON COLUMN t_integration_moka_person.deactivated        IS 'Moka API deactivated：0-不禁用 1-禁用。注意：首次创建用户传 1 则不会创建成功';
COMMENT ON COLUMN t_integration_moka_person.locale             IS 'Moka API locale：zh-CN 默认中文';
COMMENT ON COLUMN t_integration_moka_person.timezone           IS 'Moka API timezone：Asia/Shanghai 默认东八区';
COMMENT ON COLUMN t_integration_moka_person.moka_sync_status   IS 'Moka 开放平台同步状态：0-PENDING 1-SYNCED 2-SYNC_FAILED';
COMMENT ON COLUMN t_integration_moka_person.last_sync_time     IS '最近一次推送 Moka 的时间';
COMMENT ON COLUMN t_integration_moka_person.last_sync_result   IS '最近一次推送结果摘要';
COMMENT ON COLUMN t_integration_moka_person.created_at         IS '记录创建时间';
COMMENT ON COLUMN t_integration_moka_person.updated_at         IS '记录更新时间';
COMMENT ON COLUMN t_integration_moka_person.is_deleted         IS '逻辑删除：0-正常 1-删除';

-- 常用查询索引
CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_email
    ON t_integration_moka_person(company_email) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_sync_status
    ON t_integration_moka_person(moka_sync_status) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_emp_status
    ON t_integration_moka_person(employee_status) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_employee_no
    ON t_integration_moka_person(employee_no) WHERE is_deleted = 0;
