-- =============================================
-- V41: 部门主数据治理（MDM）iHR 部门同步 4 张表
--   链路: stg_ihr_dept(贴源) → ods_ihr_dept(规范化) → mdm_dept(部门主档)
--         + mdm_dept_event(事件)
--   表前缀 md_/stg_ 为 MDM 数据治理专用（与 V35 人员链路同约定）
-- =============================================

-- ---------- 1. stg_ihr_dept 贴源层：iHR 原始报文 ----------
CREATE TABLE IF NOT EXISTS stg_ihr_dept (
    id            BIGINT       NOT NULL,                -- 内部主键（MyBatis-Plus 雪花生成）
    etl_batch_no  VARCHAR(64)  NOT NULL,                -- ETL 批次号（每轮拉取生成，DEPT+yyyyMMddHHmmssSSS）
    ihr_dept_uuid VARCHAR(64)  NOT NULL,                -- iHR 部门 UUID（业务唯一标识）
    raw_json      JSONB,                                 -- iHR 原始响应报文（含未映射字段，贴源留底）
    http_status   SMALLINT,                              -- 拉取时 HTTP 状态码（成功路径恒 200）
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted    SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_stg_ihr_dept PRIMARY KEY (id),
    CONSTRAINT uk_stg_ihr_dept_batch_uuid UNIQUE (etl_batch_no, ihr_dept_uuid)
);

COMMENT ON TABLE  stg_ihr_dept             IS 'iHR部门信息贴源表';
COMMENT ON COLUMN stg_ihr_dept.id          IS '内部主键';
COMMENT ON COLUMN stg_ihr_dept.etl_batch_no IS 'ETL批次号';
COMMENT ON COLUMN stg_ihr_dept.ihr_dept_uuid IS 'iHR部门UUID';
COMMENT ON COLUMN stg_ihr_dept.raw_json    IS 'iHR原始响应报文';
COMMENT ON COLUMN stg_ihr_dept.http_status IS '拉取时HTTP状态码';
COMMENT ON COLUMN stg_ihr_dept.created_at  IS '记录创建时间';
COMMENT ON COLUMN stg_ihr_dept.updated_at  IS '记录更新时间';
COMMENT ON COLUMN stg_ihr_dept.is_deleted  IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_stg_ihr_dept_batch
    ON stg_ihr_dept(etl_batch_no);

-- ---------- 2. ods_ihr_dept 规范化层：字段清洗 + 行级幂等 ----------
CREATE TABLE IF NOT EXISTS ods_ihr_dept (
    id                     BIGINT       NOT NULL,       -- 内部主键（雪花生成）
    ihr_dept_uuid          VARCHAR(64)  NOT NULL,       -- iHR 部门 UUID（业务主键，UNIQUE）
    ihr_dept_id            VARCHAR(64),                 -- iHR 部门 ID（数字，存字符串形式）
    department_code        VARCHAR(64),                 -- 部门编码
    dept_name              VARCHAR(256),                -- 部门名称
    parent_id              VARCHAR(64),                 -- 上级部门 ID（数字，存字符串形式；无上级时 NULL）
    parent_department_code VARCHAR(64),                 -- 上级部门编码（无上级时 NULL）
    dept_type              VARCHAR(32),                 -- 部门类型：COMPANY / DIVISION / DEPARTMENT 等
    store_number           VARCHAR(64),                 -- 门店编号
    principal_staff_id     VARCHAR(64),                 -- 部门负责人 staffId
    dept_status            VARCHAR(32),                 -- 部门状态：ENABLE 等
    is_virtual             SMALLINT,                    -- 是否虚拟节点：0-否 1-是
    cost_name              VARCHAR(128),                -- 成本中心（根据部门id获取部门详情接口返回）
    cost_code              VARCHAR(64),                 -- 成本编码（根据部门id获取部门详情接口返回）
    company_site_id        VARCHAR(64),                 -- 工作地点 id（根据部门id获取部门详情接口返回）
    company_site_name      VARCHAR(128),                -- 工作地点名称（根据部门id获取部门详情接口返回）
    sequence               INT,                         -- 排序号
    row_hash               VARCHAR(64)  NOT NULL,       -- 变更检测哈希（SHA-256，未变则幂等短路）
    ver_no                 INT          NOT NULL DEFAULT 1,  -- 数据版本号（每次内容变更 +1）
    dq_flag                SMALLINT     NOT NULL DEFAULT 0,  -- 数据质量标记：0-正常 1-存在质量问题
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted             SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_ods_ihr_dept PRIMARY KEY (id),
    CONSTRAINT uk_ods_ihr_dept_uuid UNIQUE (ihr_dept_uuid)
);

COMMENT ON TABLE  ods_ihr_dept                     IS 'iHR部门信息规范化表';
COMMENT ON COLUMN ods_ihr_dept.id                  IS '内部主键';
COMMENT ON COLUMN ods_ihr_dept.ihr_dept_uuid       IS 'iHR部门UUID';
COMMENT ON COLUMN ods_ihr_dept.ihr_dept_id         IS 'iHR部门ID';
COMMENT ON COLUMN ods_ihr_dept.department_code     IS '部门编码';
COMMENT ON COLUMN ods_ihr_dept.dept_name           IS '部门名称';
COMMENT ON COLUMN ods_ihr_dept.parent_id           IS '上级部门ID';
COMMENT ON COLUMN ods_ihr_dept.parent_department_code IS '上级部门编码';
COMMENT ON COLUMN ods_ihr_dept.dept_type           IS '部门类型';
COMMENT ON COLUMN ods_ihr_dept.store_number        IS '门店编号';
COMMENT ON COLUMN ods_ihr_dept.principal_staff_id  IS '部门负责人staffId';
COMMENT ON COLUMN ods_ihr_dept.dept_status         IS '部门状态';
COMMENT ON COLUMN ods_ihr_dept.is_virtual          IS '是否虚拟节点：0-否 1-是';
COMMENT ON COLUMN ods_ihr_dept.cost_name           IS '成本中心';
COMMENT ON COLUMN ods_ihr_dept.cost_code           IS '成本编码';
COMMENT ON COLUMN ods_ihr_dept.company_site_id     IS '工作地点id';
COMMENT ON COLUMN ods_ihr_dept.company_site_name   IS '工作地点名称';
COMMENT ON COLUMN ods_ihr_dept.sequence            IS '排序号';
COMMENT ON COLUMN ods_ihr_dept.row_hash            IS '变更检测哈希';
COMMENT ON COLUMN ods_ihr_dept.ver_no              IS '数据版本号';
COMMENT ON COLUMN ods_ihr_dept.dq_flag             IS '数据质量标记：0-正常 1-存在质量问题';
COMMENT ON COLUMN ods_ihr_dept.created_at          IS '记录创建时间';
COMMENT ON COLUMN ods_ihr_dept.updated_at          IS '记录更新时间';
COMMENT ON COLUMN ods_ihr_dept.is_deleted          IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_ods_ihr_dept_dept_id
    ON ods_ihr_dept(ihr_dept_id);

CREATE INDEX IF NOT EXISTS idx_ods_ihr_dept_code
    ON ods_ihr_dept(department_code);

-- ---------- 3. mdm_dept 主数据层：部门主档 ----------
CREATE TABLE IF NOT EXISTS mdm_dept (
    id                 BIGINT       NOT NULL,           -- 内部主键（雪花生成）
    dept_uuid          VARCHAR(64)  NOT NULL,           -- iHR 部门 UUID（来源系统唯一标识，UNIQUE）
    dept_code          VARCHAR(64),                     -- 部门编码（取 iHR departmentCode，覆盖更新）
    dept_name          VARCHAR(256) NOT NULL,           -- 部门名称
    parent_dept_code   VARCHAR(64),                     -- 上级部门编码（取 iHR parentDepartmentCode，允许乱序建档）
    dept_type          VARCHAR(32),                     -- 部门类型：COMPANY / DIVISION / DEPARTMENT 等
    dept_status        VARCHAR(32),                     -- 部门状态：ENABLE 等
    principal_staff_id VARCHAR(64),                     -- 部门负责人 staffId
    sequence           INT,                             -- 排序号
    src_sys_code       VARCHAR(32)  NOT NULL,           -- 首次建档来源系统：IHR
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted         SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_mdm_dept PRIMARY KEY (id),
    CONSTRAINT uk_mdm_dept_uuid UNIQUE (dept_uuid)
);

COMMENT ON TABLE  mdm_dept                 IS '部门主数据主档表';
COMMENT ON COLUMN mdm_dept.id              IS '内部主键';
COMMENT ON COLUMN mdm_dept.dept_uuid       IS 'iHR部门UUID';
COMMENT ON COLUMN mdm_dept.dept_code       IS '部门编码';
COMMENT ON COLUMN mdm_dept.dept_name       IS '部门名称';
COMMENT ON COLUMN mdm_dept.parent_dept_code IS '上级部门编码';
COMMENT ON COLUMN mdm_dept.dept_type       IS '部门类型';
COMMENT ON COLUMN mdm_dept.dept_status     IS '部门状态';
COMMENT ON COLUMN mdm_dept.principal_staff_id IS '部门负责人staffId';
COMMENT ON COLUMN mdm_dept.sequence        IS '排序号';
COMMENT ON COLUMN mdm_dept.src_sys_code    IS '首次建档来源系统：IHR';
COMMENT ON COLUMN mdm_dept.created_at      IS '记录创建时间';
COMMENT ON COLUMN mdm_dept.updated_at      IS '记录更新时间';
COMMENT ON COLUMN mdm_dept.is_deleted      IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_mdm_dept_parent_code
    ON mdm_dept(parent_dept_code);

-- ---------- 4. mdm_dept_event 主数据层：部门变更事件 ----------
CREATE TABLE IF NOT EXISTS mdm_dept_event (
    id           BIGINT       NOT NULL,                 -- 内部主键（雪花生成）
    event_id     VARCHAR(64)  NOT NULL,                 -- 事件幂等键 SHA-256(deptUuid:eventType:rowHash)（UNIQUE）
    event_type   VARCHAR(32)  NOT NULL,                 -- 事件类型：CREATE-新建 UPDATE-更新 DISABLE-停用
    change_level VARCHAR(16)  NOT NULL,                 -- 变更级别：HIGH-新建/停用 NORMAL-更新
    dept_id      BIGINT,                                -- 关联 mdm_dept.id
    dept_code    VARCHAR(64),                           -- 事件发生时部门编码（快照）
    payload      JSONB,                                 -- 事件明细 JSON（deptId/deptUuid/deptCode/deptName/etlBatchNo）
    etl_batch_no VARCHAR(64),                           -- 产生本事件的 ETL 批次号
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- 处理状态：PENDING-待消费
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted   SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_mdm_dept_event PRIMARY KEY (id),
    CONSTRAINT uk_mdm_dept_event_id UNIQUE (event_id)
);

COMMENT ON TABLE  mdm_dept_event            IS '部门主数据变更事件表';
COMMENT ON COLUMN mdm_dept_event.id         IS '内部主键';
COMMENT ON COLUMN mdm_dept_event.event_id   IS '事件幂等键SHA-256';
COMMENT ON COLUMN mdm_dept_event.event_type IS '事件类型：CREATE-新建 UPDATE-更新 DISABLE-停用';
COMMENT ON COLUMN mdm_dept_event.change_level IS '变更级别：HIGH-新建/停用 NORMAL-更新';
COMMENT ON COLUMN mdm_dept_event.dept_id    IS '关联mdm_dept.id';
COMMENT ON COLUMN mdm_dept_event.dept_code  IS '事件发生时部门编码快照';
COMMENT ON COLUMN mdm_dept_event.payload    IS '事件明细JSON';
COMMENT ON COLUMN mdm_dept_event.etl_batch_no IS '产生本事件的ETL批次号';
COMMENT ON COLUMN mdm_dept_event.status     IS '处理状态：PENDING-待消费';
COMMENT ON COLUMN mdm_dept_event.created_at IS '记录创建时间';
COMMENT ON COLUMN mdm_dept_event.updated_at IS '记录更新时间';
COMMENT ON COLUMN mdm_dept_event.is_deleted IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_mdm_dept_event_status
    ON mdm_dept_event(status) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_mdm_dept_event_dept_id
    ON mdm_dept_event(dept_id);
