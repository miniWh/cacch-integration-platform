-- =============================================
-- V25: IHR 开放平台职位快照表
--   对接 iHR 开放平台「获取公司职位清单」接口
--   GET /api/v1/org/{orgId}/positions
--   全量快照同步，uuid（iHR 职位 id）为业务主键
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_ihr_position (
    -- 内部主键（MyBatis-Plus 雪花生成；uuid 为业务主键，单独建 UNIQUE 约束）
    id                      BIGINT       NOT NULL,
    -- ========== iHR 业务字段（与接口返回一一对应，字段命名对齐接口 + 蛇形规范） ==========
    uuid                    VARCHAR(64)  NOT NULL,   -- 职位ID（iHR 侧业务主键，UNIQUE；文档声明 Long，存 VARCHAR 兼容）
    company_id              VARCHAR(32),             -- 公司ID（iHR 文档声明 Long，存 VARCHAR 兼容）
    position_name           VARCHAR(128) NOT NULL,   -- 职位名称
    abbreviation            VARCHAR(64),              -- 简称
    position_code           VARCHAR(128),             -- 职位编码
    apply_range             VARCHAR(64),              -- 适用范围（如 CURRENT_DEPARTMENT）
    capacity                INTEGER,                  -- 编制人数
    affective_date          VARCHAR(32),              -- 生效日期（iHR 返回 String，格式 YYYY-MM-DD）
    expiry_date             VARCHAR(32),              -- 失效日期（iHR 返回 String，格式 YYYY-MM-DD）
    position_scope          VARCHAR(64),              -- 职位范围
    description             VARCHAR(1000),             -- 职位描述
    department_id           VARCHAR(32),              -- 部门ID（iHR 文档声明 Long，存 VARCHAR 兼容）
    department_name         VARCHAR(128),             -- 部门名称
    job_function            VARCHAR(64),              -- 职能
    job_sub_function        VARCHAR(64),              -- 子职能
    position_graded         BOOLEAN,                   -- 是否定级
    position_grade_name     VARCHAR(64),              -- 职级名称
    qualification           VARCHAR(500),             -- 任职资格
    parent_id               VARCHAR(32),              -- 上级职位ID（iHR 文档声明 Long，存 VARCHAR 兼容；null 表示无上级）
    is_position_group       BOOLEAN,                   -- 是否职位组
    position_state          SMALLINT,                  -- 职位状态：0-停用 1-启用（请求参数 positionState 过滤）
    position_state_string   VARCHAR(16),              -- 职位状态描述（如 "启用" / "停用"）
    update_date             VARCHAR(32),              -- iHR 更新时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
    create_date             VARCHAR(32),              -- iHR 创建时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
    -- ========== 审计/辅助字段 ==========
    sync_batch              VARCHAR(64),              -- 同步批次号（每次全量/增量同步生成唯一值，便于追溯）
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_ihr_position PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_ihr_position_uuid UNIQUE (uuid)
);

COMMENT ON TABLE  t_integration_ihr_position IS 'IHR职位信息表';

COMMENT ON COLUMN t_integration_ihr_position.id                     IS '内部主键（雪花生成）';
COMMENT ON COLUMN t_integration_ihr_position.uuid                   IS 'iHR 职位ID（业务主键，UNIQUE）';
COMMENT ON COLUMN t_integration_ihr_position.company_id             IS '公司ID（iHR 文档声明 Long，存 VARCHAR 兼容）';
COMMENT ON COLUMN t_integration_ihr_position.position_name          IS '职位名称';
COMMENT ON COLUMN t_integration_ihr_position.abbreviation           IS '简称';
COMMENT ON COLUMN t_integration_ihr_position.position_code          IS '职位编码';
COMMENT ON COLUMN t_integration_ihr_position.apply_range            IS '适用范围（如 CURRENT_DEPARTMENT）';
COMMENT ON COLUMN t_integration_ihr_position.capacity               IS '编制人数';
COMMENT ON COLUMN t_integration_ihr_position.affective_date          IS '生效日期（iHR 返回 String，格式 YYYY-MM-DD）';
COMMENT ON COLUMN t_integration_ihr_position.expiry_date             IS '失效日期（iHR 返回 String，格式 YYYY-MM-DD）';
COMMENT ON COLUMN t_integration_ihr_position.position_scope         IS '职位范围';
COMMENT ON COLUMN t_integration_ihr_position.description             IS '职位描述';
COMMENT ON COLUMN t_integration_ihr_position.department_id           IS '部门ID（iHR 文档声明 Long，存 VARCHAR 兼容）';
COMMENT ON COLUMN t_integration_ihr_position.department_name         IS '部门名称';
COMMENT ON COLUMN t_integration_ihr_position.job_function            IS '职能';
COMMENT ON COLUMN t_integration_ihr_position.job_sub_function        IS '子职能';
COMMENT ON COLUMN t_integration_ihr_position.position_graded        IS '是否已定级';
COMMENT ON COLUMN t_integration_ihr_position.position_grade_name    IS '职级名称';
COMMENT ON COLUMN t_integration_ihr_position.qualification          IS '任职资格';
COMMENT ON COLUMN t_integration_ihr_position.parent_id              IS '上级职位ID（null 表示无上级）';
COMMENT ON COLUMN t_integration_ihr_position.is_position_group      IS '是否职位组';
COMMENT ON COLUMN t_integration_ihr_position.position_state         IS '职位状态：0-停用 1-启用';
COMMENT ON COLUMN t_integration_ihr_position.position_state_string  IS '职位状态描述';
COMMENT ON COLUMN t_integration_ihr_position.update_date            IS 'iHR 更新时间（原始 String）';
COMMENT ON COLUMN t_integration_ihr_position.create_date             IS 'iHR 创建时间（原始 String）';
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
