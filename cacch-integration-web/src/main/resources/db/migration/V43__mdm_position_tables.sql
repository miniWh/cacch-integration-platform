-- =============================================
-- V43: 岗位主数据治理（MDM）iHR 岗位同步 5 张表
--   链路: stg_ihr_position(贴源) → ods_ihr_position(规范化) → mdm_position(岗位主档)
--         + mdm_position_event(事件) + mdm_position_src_ref(来源映射)
--   表前缀 md_/stg_ 为 MDM 数据治理专用（与 V35 人员链路、V41 部门链路同约定）
--   数据来源: ORG_POSITION_LIST_PATH 全量岗位清单（无分页）
--            + positionIds 按职位ID批量查询（员工变更职位保障用）
-- =============================================

-- ---------- 1. stg_ihr_position 贴源层：iHR 原始报文 ----------
CREATE TABLE IF NOT EXISTS stg_ihr_position (
    id                BIGINT       NOT NULL,                -- 内部主键（MyBatis-Plus 雪花生成）
    etl_batch_no      VARCHAR(64)  NOT NULL,                -- ETL 批次号（每轮拉取生成，POS+yyyyMMddHHmmssSSS）
    ihr_position_uuid VARCHAR(64)  NOT NULL,                -- iHR 职位 UUID（业务唯一标识）
    raw_json          JSONB,                                 -- iHR 原始响应报文（含未映射字段，贴源留底）
    http_status       SMALLINT,                              -- 拉取时 HTTP 状态码（成功路径恒 200）
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted        SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_stg_ihr_position PRIMARY KEY (id),
    CONSTRAINT uk_stg_ihr_position_batch_uuid UNIQUE (etl_batch_no, ihr_position_uuid)
);

COMMENT ON TABLE  stg_ihr_position                 IS 'iHR岗位信息贴源表';
COMMENT ON COLUMN stg_ihr_position.id              IS '内部主键';
COMMENT ON COLUMN stg_ihr_position.etl_batch_no    IS 'ETL批次号';
COMMENT ON COLUMN stg_ihr_position.ihr_position_uuid IS 'iHR职位UUID';
COMMENT ON COLUMN stg_ihr_position.raw_json        IS 'iHR原始响应报文';
COMMENT ON COLUMN stg_ihr_position.http_status     IS '拉取时HTTP状态码';
COMMENT ON COLUMN stg_ihr_position.created_at      IS '记录创建时间';
COMMENT ON COLUMN stg_ihr_position.updated_at      IS '记录更新时间';
COMMENT ON COLUMN stg_ihr_position.is_deleted      IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_stg_ihr_position_batch
    ON stg_ihr_position(etl_batch_no);

-- ---------- 2. ods_ihr_position 规范化层：字段清洗 + 行级幂等 ----------
CREATE TABLE IF NOT EXISTS ods_ihr_position (
    id                    BIGINT       NOT NULL,            -- 内部主键（雪花生成）
    ihr_position_uuid     VARCHAR(64)  NOT NULL,            -- iHR 职位 UUID（业务主键，UNIQUE）
    company_id            VARCHAR(64),                      -- 公司 ID（String UUID）
    position_name         VARCHAR(128) NOT NULL,            -- 职位名称（iHR 最大长度100，放宽存储）
    abbreviation          VARCHAR(128),                     -- 简称
    position_code         VARCHAR(64),                      -- 职位编号（iHR 最大长度50）
    applied_range         VARCHAR(64),                      -- 应用范围：CURRENT_DEPARTMENT / CURRENT_DEPARTMENT_AND_CHILDREN / ALL_DEPARTMENT
    capacity              INTEGER,                          -- 当前编制人数
    effective_date        VARCHAR(32),                      -- 生效日期（iHR 返回 String，格式 YYYY-MM-DD）
    description           VARCHAR(500),                     -- 职位描述（iHR 最大长度255，放宽存储）
    department_id         VARCHAR(64),                      -- 所属部门 ID（iHR 返回 Long，存字符串形式）
    department_name       VARCHAR(256),                     -- 所属部门名称（iHR 最大长度128）
    job_title_id          VARCHAR(64),                      -- 对应职务 ID（String UUID）
    job_title_name        VARCHAR(128),                     -- 对应职务名称
    position_grade_id     VARCHAR(64),                      -- 对应职级 ID（String UUID）
    position_grade_name   VARCHAR(64),                      -- 对应职级名称
    qualifications        VARCHAR(500),                     -- 任职资格
    parent_id             VARCHAR(64),                      -- 父级编号（String UUID；null 表示无上级）
    is_position_group     SMALLINT,                         -- 是否为职位组：0-否 1-是
    position_state        VARCHAR(16),                      -- 职位状态：ENABLE-启用 / DISABLE-停用
    position_state_string VARCHAR(64),                      -- 职位状态描述
    updated_date          VARCHAR(32),                      -- iHR 更新时间（原始 String）
    created_date          VARCHAR(32),                      -- iHR 创建时间（原始 String）
    position_scope        JSONB,                            -- 应用范围部门 ID 列表（iHR 返回 List，存 JSONB）
    row_hash              VARCHAR(64)  NOT NULL,            -- 变更检测哈希（SHA-256，未变则幂等短路）
    ver_no                INT          NOT NULL DEFAULT 1,  -- 数据版本号（每次内容变更 +1）
    dq_flag               SMALLINT     NOT NULL DEFAULT 0,  -- 数据质量标记：0-正常 1-存在质量问题
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted            SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_ods_ihr_position PRIMARY KEY (id),
    CONSTRAINT uk_ods_ihr_position_uuid UNIQUE (ihr_position_uuid)
);

COMMENT ON TABLE  ods_ihr_position                     IS 'iHR岗位信息规范化表';
COMMENT ON COLUMN ods_ihr_position.id                  IS '内部主键';
COMMENT ON COLUMN ods_ihr_position.ihr_position_uuid   IS 'iHR职位UUID';
COMMENT ON COLUMN ods_ihr_position.company_id          IS '公司ID（String UUID）';
COMMENT ON COLUMN ods_ihr_position.position_name       IS '职位名称（最大长度100）';
COMMENT ON COLUMN ods_ihr_position.abbreviation        IS '简称（最大长度100）';
COMMENT ON COLUMN ods_ihr_position.position_code       IS '职位编号（最大长度50）';
COMMENT ON COLUMN ods_ihr_position.applied_range       IS '应用范围：CURRENT_DEPARTMENT/CURRENT_DEPARTMENT_AND_CHILDREN/ALL_DEPARTMENT';
COMMENT ON COLUMN ods_ihr_position.capacity            IS '当前编制人数';
COMMENT ON COLUMN ods_ihr_position.effective_date      IS '生效日期（iHR 返回 String，格式 YYYY-MM-DD）';
COMMENT ON COLUMN ods_ihr_position.description         IS '职位描述（最大长度255）';
COMMENT ON COLUMN ods_ihr_position.department_id       IS '所属部门ID（iHR 返回 Long，存字符串形式）';
COMMENT ON COLUMN ods_ihr_position.department_name     IS '所属部门名称（最大长度128）';
COMMENT ON COLUMN ods_ihr_position.job_title_id        IS '对应职务ID（String UUID）';
COMMENT ON COLUMN ods_ihr_position.job_title_name      IS '对应职务名称';
COMMENT ON COLUMN ods_ihr_position.position_grade_id   IS '对应职级ID（String UUID）';
COMMENT ON COLUMN ods_ihr_position.position_grade_name IS '对应职级名称';
COMMENT ON COLUMN ods_ihr_position.qualifications      IS '任职资格';
COMMENT ON COLUMN ods_ihr_position.parent_id           IS '父级编号（null 表示无上级）';
COMMENT ON COLUMN ods_ihr_position.is_position_group   IS '是否为职位组：0-否 1-是';
COMMENT ON COLUMN ods_ihr_position.position_state      IS '职位状态：ENABLE/DISABLE';
COMMENT ON COLUMN ods_ihr_position.position_state_string IS '职位状态描述';
COMMENT ON COLUMN ods_ihr_position.updated_date        IS 'iHR更新时间（原始 String）';
COMMENT ON COLUMN ods_ihr_position.created_date        IS 'iHR创建时间（原始 String）';
COMMENT ON COLUMN ods_ihr_position.position_scope      IS '应用范围部门ID列表（JSONB，iHR 返回 List）';
COMMENT ON COLUMN ods_ihr_position.row_hash            IS '变更检测哈希';
COMMENT ON COLUMN ods_ihr_position.ver_no              IS '数据版本号';
COMMENT ON COLUMN ods_ihr_position.dq_flag             IS '数据质量标记：0-正常 1-存在质量问题';
COMMENT ON COLUMN ods_ihr_position.created_at          IS '记录创建时间';
COMMENT ON COLUMN ods_ihr_position.updated_at          IS '记录更新时间';
COMMENT ON COLUMN ods_ihr_position.is_deleted          IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_ods_ihr_position_code
    ON ods_ihr_position(position_code);

CREATE INDEX IF NOT EXISTS idx_ods_ihr_position_dept
    ON ods_ihr_position(department_id);

CREATE INDEX IF NOT EXISTS idx_ods_ihr_position_state
    ON ods_ihr_position(position_state) WHERE is_deleted = 0;

-- ---------- 3. mdm_position 主数据层：岗位主档 ----------
CREATE TABLE IF NOT EXISTS mdm_position (
    id                  BIGINT       NOT NULL,             -- 内部主键（雪花生成）
    position_code       VARCHAR(64),                       -- 职位编号（取 iHR positionCode，覆盖更新）
    position_name       VARCHAR(128) NOT NULL,             -- 职位名称
    parent_position_id  VARCHAR(64),                       -- 父级职位编号（取 iHR parentId，允许乱序建档）
    department_id       VARCHAR(64),                       -- 所属部门 ID（暂存 iHR departmentId 字符串形式，后续融合 md_org）
    department_name     VARCHAR(256),                      -- 所属部门名称
    job_title_id        VARCHAR(64),                       -- 对应职务 ID
    job_title_name      VARCHAR(128),                      -- 对应职务名称
    position_grade_id   VARCHAR(64),                       -- 对应职级 ID
    position_grade_name VARCHAR(64),                       -- 对应职级名称
    applied_range       VARCHAR(64),                       -- 应用范围：CURRENT_DEPARTMENT 等
    capacity            INTEGER,                           -- 当前编制人数
    effective_date      VARCHAR(32),                       -- 生效日期
    position_state      VARCHAR(16),                       -- 职位状态：ENABLE-启用 / DISABLE-停用
    src_sys_code        VARCHAR(32)  NOT NULL,             -- 首次建档来源系统：IHR
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_mdm_position PRIMARY KEY (id)
);

COMMENT ON TABLE  mdm_position                     IS '岗位主数据主档表';
COMMENT ON COLUMN mdm_position.id                  IS '内部主键';
COMMENT ON COLUMN mdm_position.position_code       IS '职位编号';
COMMENT ON COLUMN mdm_position.position_name       IS '职位名称';
COMMENT ON COLUMN mdm_position.parent_position_id  IS '父级职位编号';
COMMENT ON COLUMN mdm_position.department_id       IS '所属部门ID';
COMMENT ON COLUMN mdm_position.department_name     IS '所属部门名称';
COMMENT ON COLUMN mdm_position.job_title_id        IS '对应职务ID';
COMMENT ON COLUMN mdm_position.job_title_name      IS '对应职务名称';
COMMENT ON COLUMN mdm_position.position_grade_id   IS '对应职级ID';
COMMENT ON COLUMN mdm_position.position_grade_name IS '对应职级名称';
COMMENT ON COLUMN mdm_position.applied_range       IS '应用范围：CURRENT_DEPARTMENT / CURRENT_DEPARTMENT_AND_CHILDREN / ALL_DEPARTMENT';
COMMENT ON COLUMN mdm_position.capacity            IS '当前编制人数';
COMMENT ON COLUMN mdm_position.effective_date      IS '生效日期';
COMMENT ON COLUMN mdm_position.position_state      IS '职位状态：ENABLE-启用 / DISABLE-停用';
COMMENT ON COLUMN mdm_position.src_sys_code       IS '首次建档来源系统：IHR';
COMMENT ON COLUMN mdm_position.created_at          IS '记录创建时间';
COMMENT ON COLUMN mdm_position.updated_at          IS '记录更新时间';
COMMENT ON COLUMN mdm_position.is_deleted          IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_mdm_position_parent
    ON mdm_position(parent_position_id);

CREATE INDEX IF NOT EXISTS idx_mdm_position_dept
    ON mdm_position(department_id);

-- ---------- 4. mdm_position_event 主数据层：岗位变更事件 ----------
CREATE TABLE IF NOT EXISTS mdm_position_event (
    id           BIGINT       NOT NULL,                     -- 内部主键（雪花生成）
    event_id     VARCHAR(64)  NOT NULL,                     -- 事件幂等键 SHA-256(positionUuid:eventType:rowHash)（UNIQUE）
    event_type   VARCHAR(32)  NOT NULL,                     -- 事件类型：CREATE-新建 UPDATE-更新 DISABLE-停用
    change_level VARCHAR(16)  NOT NULL,                     -- 变更级别：HIGH-新建/停用 NORMAL-更新
    position_id  BIGINT,                                    -- 关联 mdm_position.id
    position_code VARCHAR(64),                              -- 事件发生时职位编号（快照）
    payload      JSONB,                                     -- 事件明细 JSON（positionId/positionUuid/positionCode/positionName/etlBatchNo）
    etl_batch_no VARCHAR(64),                               -- 产生本事件的 ETL 批次号
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING',   -- 处理状态：PENDING-待消费
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted   SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_mdm_position_event PRIMARY KEY (id),
    CONSTRAINT uk_mdm_position_event_id UNIQUE (event_id)
);

COMMENT ON TABLE  mdm_position_event                IS '岗位主数据变更事件表';
COMMENT ON COLUMN mdm_position_event.id             IS '内部主键';
COMMENT ON COLUMN mdm_position_event.event_id       IS '事件幂等键SHA-256';
COMMENT ON COLUMN mdm_position_event.event_type     IS '事件类型：CREATE-新建 UPDATE-更新 DISABLE-停用';
COMMENT ON COLUMN mdm_position_event.change_level   IS '变更级别：HIGH-新建/停用 NORMAL-更新';
COMMENT ON COLUMN mdm_position_event.position_id    IS '关联mdm_position.id';
COMMENT ON COLUMN mdm_position_event.position_code  IS '事件发生时职位编号快照';
COMMENT ON COLUMN mdm_position_event.payload        IS '事件明细JSON';
COMMENT ON COLUMN mdm_position_event.etl_batch_no   IS '产生本事件的ETL批次号';
COMMENT ON COLUMN mdm_position_event.status         IS '处理状态：PENDING-待消费';
COMMENT ON COLUMN mdm_position_event.created_at     IS '记录创建时间';
COMMENT ON COLUMN mdm_position_event.updated_at     IS '记录更新时间';
COMMENT ON COLUMN mdm_position_event.is_deleted     IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_mdm_position_event_status
    ON mdm_position_event(status) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_mdm_position_event_position_id
    ON mdm_position_event(position_id);

-- ---------- 5. mdm_position_src_ref 主数据层：岗位来源映射 ----------
CREATE TABLE IF NOT EXISTS mdm_position_src_ref (
    id               BIGINT       NOT NULL,                 -- 内部主键（雪花生成）
    position_id      BIGINT       NOT NULL,                 -- 关联 mdm_position.id
    src_sys_code     VARCHAR(32)  NOT NULL,                 -- 来源系统编码：IHR / WECOM / OA 等
    src_entity_type  VARCHAR(32)  NOT NULL,                 -- 源系统实体类型：POSITION
    src_entity_id    VARCHAR(128) NOT NULL,                 -- 源系统实体 ID（iHR 职位 uuid）
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_mdm_position_src_ref PRIMARY KEY (id),
    CONSTRAINT uk_mdm_position_src_ref_entity UNIQUE (src_sys_code, src_entity_type, src_entity_id),
    CONSTRAINT fk_mdm_position_src_ref_position FOREIGN KEY (position_id) REFERENCES mdm_position (id)
);

COMMENT ON TABLE  mdm_position_src_ref                 IS '岗位主数据多源映射表';
COMMENT ON COLUMN mdm_position_src_ref.id              IS '内部主键';
COMMENT ON COLUMN mdm_position_src_ref.position_id     IS '关联mdm_position.id';
COMMENT ON COLUMN mdm_position_src_ref.src_sys_code    IS '来源系统编码：IHR / WECOM / OA 等';
COMMENT ON COLUMN mdm_position_src_ref.src_entity_type IS '源系统实体类型：POSITION';
COMMENT ON COLUMN mdm_position_src_ref.src_entity_id   IS '源系统实体 ID（iHR 职位 uuid）';
COMMENT ON COLUMN mdm_position_src_ref.created_at      IS '记录创建时间';
COMMENT ON COLUMN mdm_position_src_ref.updated_at      IS '记录更新时间';
COMMENT ON COLUMN mdm_position_src_ref.is_deleted      IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_mdm_position_src_ref_position_id
    ON mdm_position_src_ref(position_id);
