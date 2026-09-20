-- =============================================
-- V22: IHR 开放平台组织架构部门表
--   对接 iHR 开放平台「获取部门清单 v3」接口
--   全量快照同步 / 增量变更同步均可，uuid 为 iHR 侧业务主键
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_ihr_department (
    -- 内部主键（MyBatis-Plus 雪花生成；uuid 为业务主键，单独建 UNIQUE 约束）
    id                      BIGINT       NOT NULL,
    -- ========== iHR 业务字段（与接口返回一一对应，字段命名对齐接口 + 蛇形规范） ==========
    uuid                    VARCHAR(64)  NOT NULL,   -- 部门主键id（iHR 侧业务主键，UNIQUE）
    ihr_dept_id             VARCHAR(32),             -- 部门ID（iHR 文档声明 Long，但存 VARCHAR 防止极端情况；DTO 用 String）
    name                    VARCHAR(128) NOT NULL,   -- 部门名称（iHR 文档注明最大长度128）
    parent_id               VARCHAR(32),             -- 上级部门ID（-1 表示无上级部门；同 ihr_dept_id 存 VARCHAR）
    type                    VARCHAR(32),              -- 部门类型：COMPANY-公司 / DEPARTMENT-部门 / STORE-门店
    department_code         VARCHAR(128),             -- 部门编码
    store_number            VARCHAR(128),             -- 门店编号
    principal_staff_id      VARCHAR(64),              -- 部门负责人ID
    parent_department_code  VARCHAR(128),             -- 上级部门编码
    parent_department_name  VARCHAR(128),             -- 上级部门名称
    virtual                 BOOLEAN,                  -- 是否虚拟节点
    department_status       VARCHAR(16),              -- 部门状态：ENABLE-启用 / DISABLE-停用
    department_desc         VARCHAR(500),             -- 组织描述
    department_property    VARCHAR(64),              -- 组织属性（codeTypeId=Enum.DepartmentProperty，需从 iHR 选值接口获取选项值匹配）
    last_update             VARCHAR(64),              -- 最后更新时间（iHR 返回 String，格式未明确，存原始值）
    created_date            TIMESTAMP,                -- 创建时间（iHR 声明 Date，实测为毫秒时间戳；同步时需 Long→TIMESTAMP 转换）
    abbreviation            VARCHAR(64),              -- 简称
    establish_date          VARCHAR(32),              -- 设立日期（iHR 返回 String）
    effective_date          VARCHAR(32),              -- 生效日期（iHR 返回 String）
    remark                  VARCHAR(500),             -- 备注
    sequence                BIGINT,                   -- 顺序
    -- ========== 审计/辅助字段 ==========
    sync_batch              VARCHAR(64),              -- 同步批次号（每次全量/增量同步生成唯一值，便于追溯）
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_ihr_department PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_ihr_department_uuid UNIQUE (uuid)
);

COMMENT ON TABLE  t_integration_ihr_department IS 'IHR开放平台组织架构部门快照表';

COMMENT ON COLUMN t_integration_ihr_department.id                     IS '内部主键（雪花生成）';
COMMENT ON COLUMN t_integration_ihr_department.uuid                  IS 'iHR 部门主键id（业务主键，UNIQUE）';
COMMENT ON COLUMN t_integration_ihr_department.ihr_dept_id            IS 'iHR 侧部门ID（存 VARCHAR 兼容极端情况；文档声明 Long）';
COMMENT ON COLUMN t_integration_ihr_department.name                  IS '部门名称（最大长度128）';
COMMENT ON COLUMN t_integration_ihr_department.parent_id             IS '上级部门ID（-1 表示无上级部门）';
COMMENT ON COLUMN t_integration_ihr_department.type                   IS '部门类型：COMPANY-公司 DEPARTMENT-部门 STORE-门店';
COMMENT ON COLUMN t_integration_ihr_department.department_code        IS '部门编码';
COMMENT ON COLUMN t_integration_ihr_department.store_number           IS '门店编号';
COMMENT ON COLUMN t_integration_ihr_department.principal_staff_id     IS '部门负责人ID';
COMMENT ON COLUMN t_integration_ihr_department.parent_department_code IS '上级部门编码';
COMMENT ON COLUMN t_integration_ihr_department.parent_department_name IS '上级部门名称';
COMMENT ON COLUMN t_integration_ihr_department.virtual                IS '是否虚拟节点';
COMMENT ON COLUMN t_integration_ihr_department.department_status      IS '部门状态：ENABLE-启用 DISABLE-停用';
COMMENT ON COLUMN t_integration_ihr_department.department_desc       IS '组织描述';
COMMENT ON COLUMN t_integration_ihr_department.department_property   IS '组织属性（需从 iHR 选值接口获取选项值匹配）';
COMMENT ON COLUMN t_integration_ihr_department.last_update            IS 'iHR 最后更新时间（原始 String）';
COMMENT ON COLUMN t_integration_ihr_department.created_date           IS 'iHR 创建时间（同步时需 Long→TIMESTAMP 转换）';
COMMENT ON COLUMN t_integration_ihr_department.abbreviation           IS '简称';
COMMENT ON COLUMN t_integration_ihr_department.establish_date         IS '设立日期';
COMMENT ON COLUMN t_integration_ihr_department.effective_date         IS '生效日期';
COMMENT ON COLUMN t_integration_ihr_department.remark                 IS '备注';
COMMENT ON COLUMN t_integration_ihr_department.sequence               IS '顺序';
COMMENT ON COLUMN t_integration_ihr_department.sync_batch             IS '同步批次号（便于追溯每次同步写入的记录）';
COMMENT ON COLUMN t_integration_ihr_department.created_at             IS '记录创建时间';
COMMENT ON COLUMN t_integration_ihr_department.updated_at             IS '记录更新时间';
COMMENT ON COLUMN t_integration_ihr_department.is_deleted              IS '逻辑删除：0-正常 1-删除';

-- 常用查询索引
CREATE INDEX IF NOT EXISTS idx_t_integration_ihr_dept_parent
    ON t_integration_ihr_department(parent_id) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_ihr_dept_type_status
    ON t_integration_ihr_department(type, department_status) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_ihr_dept_code
    ON t_integration_ihr_department(department_code) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_ihr_dept_sync_batch
    ON t_integration_ihr_department(sync_batch) WHERE is_deleted = 0;
