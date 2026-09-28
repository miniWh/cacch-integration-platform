-- =============================================
-- V25: IHR 开放平台职位快照表
--   对接 iHR 开放平台「获取公司职位清单」接口
--   GET /openapi/thirdparty/api/org/v1/organizations/positions（无 orgId 路径参数）
--   全量快照同步，uuid（iHR 职位 id）为业务主键
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_ihr_position (
    -- 内部主键（MyBatis-Plus 雪花生成；uuid 为业务主键，单独建 UNIQUE 约束）
    id                      BIGINT       NOT NULL,
    -- ========== iHR 业务字段（严格对齐 2026-09-28 在线文档，蛇形规范） ==========
    uuid                    VARCHAR(64)  NOT NULL,   -- 职位ID（String UUID，iHR 侧业务主键，UNIQUE）
    company_id              VARCHAR(64),             -- 公司ID（String UUID）
    position_name           VARCHAR(128) NOT NULL,   -- 职位名称（iHR 最大长度100，放宽存储）
    abbreviation            VARCHAR(128),            -- 简称
    position_code           VARCHAR(64),             -- 职位编号（iHR 最大长度50）
    applied_range           VARCHAR(64),             -- 应用范围：CURRENT_DEPARTMENT / CURRENT_DEPARTMENT_AND_CHILDREN / ALL_DEPARTMENT
    capacity                INTEGER,                  -- 当前编制人数
    effective_date          VARCHAR(32),             -- 生效日期（iHR 返回 String，格式 YYYY-MM-DD）
    description             VARCHAR(500),            -- 职位描述（iHR 最大长度255，放宽存储）
    department_id           VARCHAR(32),             -- 所属部门id（Long 存 VARCHAR 兼容）
    department_name         VARCHAR(256),            -- 所属部门名称（iHR 最大长度128）
    job_title_id            VARCHAR(64),             -- 对应职务id（String UUID）
    job_title_name          VARCHAR(128),            -- 对应职务名称
    position_grade_id       VARCHAR(64),             -- 对应职级id（String UUID）
    position_grade_name     VARCHAR(64),             -- 对应职级名称
    qualifications          VARCHAR(500),            -- 任职资格
    parent_id               VARCHAR(64),             -- 父级编号（String UUID；null 表示无上级）
    is_position_group       BOOLEAN,                  -- 是否为职位组
    position_state          VARCHAR(16),             -- 职位状态：ENABLE / DISABLE（String 非 Integer）
    position_state_string   VARCHAR(16),             -- 职位状态描述
    updated_date            VARCHAR(32),             -- iHR 更新时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
    created_date            VARCHAR(32),             -- iHR 创建时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
    position_scope          JSONB,                   -- 应用范围部门id列表（iHR 返回 List<Integer>，存 JSONB）
    -- ========== 审计/辅助字段 ==========
    sync_batch              VARCHAR(64),             -- 同步批次号（每次全量/增量同步生成唯一值，便于追溯）
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_ihr_position PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_ihr_position_uuid UNIQUE (uuid)
);

COMMENT ON TABLE  t_integration_ihr_position IS 'IHR职位信息表';

COMMENT ON COLUMN t_integration_ihr_position.id                     IS '内部主键（雪花生成）';
COMMENT ON COLUMN t_integration_ihr_position.uuid                   IS 'iHR 职位ID（业务主键，UNIQUE；String UUID）';
COMMENT ON COLUMN t_integration_ihr_position.company_id             IS '公司ID（String UUID）';
COMMENT ON COLUMN t_integration_ihr_position.position_name          IS '职位名称（最大长度100）';
COMMENT ON COLUMN t_integration_ihr_position.abbreviation           IS '简称（最大长度100）';
COMMENT ON COLUMN t_integration_ihr_position.position_code          IS '职位编号（最大长度50）';
COMMENT ON COLUMN t_integration_ihr_position.applied_range          IS '应用范围：CURRENT_DEPARTMENT/CURRENT_DEPARTMENT_AND_CHILDREN/ALL_DEPARTMENT';
COMMENT ON COLUMN t_integration_ihr_position.capacity               IS '当前编制人数';
COMMENT ON COLUMN t_integration_ihr_position.effective_date          IS '生效日期（iHR 返回 String，格式 YYYY-MM-DD）';
COMMENT ON COLUMN t_integration_ihr_position.description             IS '职位描述（最大长度255）';
COMMENT ON COLUMN t_integration_ihr_position.department_id           IS '所属部门id（iHR 返回 Long）';
COMMENT ON COLUMN t_integration_ihr_position.department_name         IS '所属部门名称（最大长度128）';
COMMENT ON COLUMN t_integration_ihr_position.job_title_id            IS '对应职务id（String UUID）';
COMMENT ON COLUMN t_integration_ihr_position.job_title_name          IS '对应职务名称';
COMMENT ON COLUMN t_integration_ihr_position.position_grade_id      IS '对应职级id（String UUID）';
COMMENT ON COLUMN t_integration_ihr_position.position_grade_name    IS '对应职级名称';
COMMENT ON COLUMN t_integration_ihr_position.qualifications          IS '任职资格';
COMMENT ON COLUMN t_integration_ihr_position.parent_id              IS '父级编号（null 表示无上级）';
COMMENT ON COLUMN t_integration_ihr_position.is_position_group      IS '是否为职位组';
COMMENT ON COLUMN t_integration_ihr_position.position_state         IS '职位状态：ENABLE/DISABLE';
COMMENT ON COLUMN t_integration_ihr_position.position_state_string  IS '职位状态描述';
COMMENT ON COLUMN t_integration_ihr_position.updated_date            IS 'iHR 更新时间（原始 String）';
COMMENT ON COLUMN t_integration_ihr_position.created_date             IS 'iHR 创建时间（原始 String）';
COMMENT ON COLUMN t_integration_ihr_position.position_scope         IS '应用范围部门id列表（JSONB，iHR 返回 List<Integer>）';
COMMENT ON COLUMN t_integration_ihr_position.sync_batch             IS '同步批次号（便于追溯每次同步写入的记录）';
COMMENT ON COLUMN t_integration_ihr_position.created_at              IS '记录创建时间';
COMMENT ON COLUMN t_integration_ihr_position.updated_at              IS '记录更新时间';
COMMENT ON COLUMN t_integration_ihr_position.is_deleted              IS '逻辑删除：0-正常 1-删除';

-- 常用查询索引
CREATE INDEX IF NOT EXISTS idx_t_integration_ihr_position_parent
    ON t_integration_ihr_position(parent_id) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_ihr_position_dept
    ON t_integration_ihr_position(department_id) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_ihr_position_state
    ON t_integration_ihr_position(position_state) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_ihr_position_code
    ON t_integration_ihr_position(position_code) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_ihr_position_sync_batch
    ON t_integration_ihr_position(sync_batch) WHERE is_deleted = 0;
