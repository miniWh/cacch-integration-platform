-- =============================================
-- V35: 人员主数据治理（MDM）iHR 人员同步 5 张表
--   链路: stg_ihr_emp(贴源) → ods_ihr_emp(规范化) → mdm_emp(身份主档)
--         + mdm_emp_org_assign(任职) + mdm_emp_event(事件)
--   表前缀 md_/sys_/stg_ 为 MDM 数据治理专用，经评审确认不使用 t_integration_ 前缀
-- =============================================

-- ---------- 1. stg_ihr_emp 贴源层：iHR 原始报文 ----------
CREATE TABLE IF NOT EXISTS stg_ihr_emp (
    id            BIGINT       NOT NULL,                -- 内部主键（MyBatis-Plus 雪花生成）
    etl_batch_no  VARCHAR(64)  NOT NULL,                -- ETL 批次号（每轮拉取生成，IHR+yyyyMMddHHmmssSSS）
    ihr_staff_id  VARCHAR(64)  NOT NULL,                -- iHR 员工 ID（UUID）
    raw_json      JSONB,                                 -- iHR 原始响应报文（含未映射字段，贴源留底）
    http_status   SMALLINT,                              -- 拉取时 HTTP 状态码（成功路径恒 200）
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted    SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_stg_ihr_emp PRIMARY KEY (id),
    CONSTRAINT uk_stg_ihr_emp_batch_staff UNIQUE (etl_batch_no, ihr_staff_id)
);

COMMENT ON TABLE  stg_ihr_emp             IS 'iHR员工信息贴源表';
COMMENT ON COLUMN stg_ihr_emp.id          IS '内部主键';
COMMENT ON COLUMN stg_ihr_emp.etl_batch_no IS 'ETL批次号';
COMMENT ON COLUMN stg_ihr_emp.ihr_staff_id IS 'iHR员工ID';
COMMENT ON COLUMN stg_ihr_emp.raw_json    IS 'iHR原始响应报文';
COMMENT ON COLUMN stg_ihr_emp.http_status IS '拉取时HTTP状态码';
COMMENT ON COLUMN stg_ihr_emp.created_at  IS '记录创建时间';
COMMENT ON COLUMN stg_ihr_emp.updated_at  IS '记录更新时间';
COMMENT ON COLUMN stg_ihr_emp.is_deleted  IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_stg_ihr_emp_batch
    ON stg_ihr_emp(etl_batch_no);

-- ---------- 2. ods_ihr_emp 规范化层：字段清洗 + 行级幂等 ----------
CREATE TABLE IF NOT EXISTS ods_ihr_emp (
    id                      BIGINT       NOT NULL,      -- 内部主键（雪花生成）
    ihr_staff_id            VARCHAR(64)  NOT NULL,      -- iHR 员工 ID（业务主键，UNIQUE）
    ihr_staff_no            VARCHAR(64),                -- 工号
    ihr_staff_name          VARCHAR(128),               -- 姓名
    ihr_mobile              VARCHAR(32),                -- 手机号
    ihr_email               VARCHAR(128),               -- 个人邮箱
    ihr_work_email          VARCHAR(128),               -- 工作邮箱
    ihr_id_card_type        VARCHAR(32),                -- 证件类型：IDENTITY_CARD / CHINA_PASSPORT 等
    ihr_id_card             VARCHAR(64),                -- 证件号码（用于 mdm_emp.id_hash 溯源）
    ihr_dept_id             VARCHAR(64),                -- 部门 ID（iHR 返回 Long，存字符串形式）
    ihr_dept_name           VARCHAR(256),               -- 部门名称
    ihr_position_id         VARCHAR(64),                -- 职位 ID
    ihr_position_name       VARCHAR(256),               -- 职位名称
    ihr_status              VARCHAR(32),                -- 员工状态：IN_SERVICE-在职 / QUIT-离职
    ihr_gender              VARCHAR(16),                -- 性别：MALE / FEMALE
    ihr_education           VARCHAR(64),                -- 最高学历
    ihr_birthday            DATE,                       -- 出生日期
    ihr_hire_date           DATE,                       -- 入职日期
    ihr_leave_date          DATE,                       -- 离职日期
    ihr_probation_end_date  DATE,                       -- 试用结束日期
    row_hash                VARCHAR(64)  NOT NULL,      -- 变更检测哈希（SHA-256，未变则幂等短路）
    ver_no                  INT          NOT NULL DEFAULT 1,  -- 数据版本号（每次内容变更 +1）
    dq_flag                 SMALLINT     NOT NULL DEFAULT 0,  -- 数据质量标记：0-正常 1-存在质量问题
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_ods_ihr_emp PRIMARY KEY (id),
    CONSTRAINT uk_ods_ihr_emp_staff_id UNIQUE (ihr_staff_id)
);

COMMENT ON TABLE  ods_ihr_emp                     IS 'iHR员工信息规范化表';
COMMENT ON COLUMN ods_ihr_emp.id                  IS '内部主键';
COMMENT ON COLUMN ods_ihr_emp.ihr_staff_id        IS 'iHR员工ID';
COMMENT ON COLUMN ods_ihr_emp.ihr_staff_no        IS '工号';
COMMENT ON COLUMN ods_ihr_emp.ihr_staff_name      IS '姓名';
COMMENT ON COLUMN ods_ihr_emp.ihr_mobile          IS '手机号';
COMMENT ON COLUMN ods_ihr_emp.ihr_email           IS '个人邮箱';
COMMENT ON COLUMN ods_ihr_emp.ihr_work_email      IS '工作邮箱';
COMMENT ON COLUMN ods_ihr_emp.ihr_id_card_type    IS '证件类型';
COMMENT ON COLUMN ods_ihr_emp.ihr_id_card         IS '证件号码';
COMMENT ON COLUMN ods_ihr_emp.ihr_dept_id         IS '部门ID';
COMMENT ON COLUMN ods_ihr_emp.ihr_dept_name       IS '部门名称';
COMMENT ON COLUMN ods_ihr_emp.ihr_position_id     IS '职位ID';
COMMENT ON COLUMN ods_ihr_emp.ihr_position_name   IS '职位名称';
COMMENT ON COLUMN ods_ihr_emp.ihr_status          IS '员工状态';
COMMENT ON COLUMN ods_ihr_emp.ihr_gender          IS '性别';
COMMENT ON COLUMN ods_ihr_emp.ihr_education       IS '最高学历';
COMMENT ON COLUMN ods_ihr_emp.ihr_birthday        IS '出生日期';
COMMENT ON COLUMN ods_ihr_emp.ihr_hire_date       IS '入职日期';
COMMENT ON COLUMN ods_ihr_emp.ihr_leave_date      IS '离职日期';
COMMENT ON COLUMN ods_ihr_emp.ihr_probation_end_date IS '试用结束日期';
COMMENT ON COLUMN ods_ihr_emp.row_hash            IS '变更检测哈希';
COMMENT ON COLUMN ods_ihr_emp.ver_no              IS '数据版本号';
COMMENT ON COLUMN ods_ihr_emp.dq_flag             IS '数据质量标记：0-正常 1-存在质量问题';
COMMENT ON COLUMN ods_ihr_emp.created_at          IS '记录创建时间';
COMMENT ON COLUMN ods_ihr_emp.updated_at          IS '记录更新时间';
COMMENT ON COLUMN ods_ihr_emp.is_deleted          IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_ods_ihr_emp_status
    ON ods_ihr_emp(ihr_status) WHERE is_deleted = 0;

-- ---------- 3. mdm_emp 主数据层：人员身份主档 ----------
CREATE TABLE IF NOT EXISTS mdm_emp (
    id              BIGINT       NOT NULL,              -- 内部主键（雪花生成）
    md_code         BIGINT       NOT NULL,              -- 人员主数据编码（雪花生成，对外稳定标识，UNIQUE）
    id_hash         VARCHAR(64)  NOT NULL,              -- 身份哈希 SHA-256(盐+证件号)（自然人唯一识别，UNIQUE）
    emp_no          VARCHAR(64),                        -- 工号（变更不断链，仅覆盖更新）
    emp_name        VARCHAR(128) NOT NULL,              -- 姓名
    gender          VARCHAR(16),                        -- 性别：MALE / FEMALE
    birthday        DATE,                               -- 出生日期
    mobile          VARCHAR(32),                        -- 手机号
    email           VARCHAR(128),                       -- 邮箱（优先工作邮箱）
    education       VARCHAR(64),                        -- 最高学历
    first_hire_date DATE,                               -- 首次入职日期（首次建档时取 iHR enrollInDate）
    src_sys_code    VARCHAR(32)  NOT NULL,              -- 首次建档来源系统：IHR
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_mdm_emp PRIMARY KEY (id),
    CONSTRAINT uk_mdm_emp_md_code UNIQUE (md_code),
    CONSTRAINT uk_mdm_emp_id_hash UNIQUE (id_hash)
);

COMMENT ON TABLE  mdm_emp              IS '人员主数据身份主档表';
COMMENT ON COLUMN mdm_emp.id           IS '内部主键';
COMMENT ON COLUMN mdm_emp.md_code      IS '人员主数据编码';
COMMENT ON COLUMN mdm_emp.id_hash      IS '身份哈希SHA-256(盐+证件号)，自然人唯一识别';
COMMENT ON COLUMN mdm_emp.emp_no       IS '工号';
COMMENT ON COLUMN mdm_emp.emp_name     IS '姓名';
COMMENT ON COLUMN mdm_emp.gender       IS '性别：MALE / FEMALE';
COMMENT ON COLUMN mdm_emp.birthday     IS '出生日期';
COMMENT ON COLUMN mdm_emp.mobile       IS '手机号';
COMMENT ON COLUMN mdm_emp.email        IS '邮箱';
COMMENT ON COLUMN mdm_emp.education    IS '最高学历';
COMMENT ON COLUMN mdm_emp.first_hire_date IS '首次入职日期';
COMMENT ON COLUMN mdm_emp.src_sys_code IS '首次建档来源系统：IHR';
COMMENT ON COLUMN mdm_emp.created_at   IS '记录创建时间';
COMMENT ON COLUMN mdm_emp.updated_at   IS '记录更新时间';
COMMENT ON COLUMN mdm_emp.is_deleted   IS '逻辑删除：0-正常 1-删除';

-- ---------- 4. mdm_emp_org_assign 主数据层：任职记录（SCD2） ----------
CREATE TABLE IF NOT EXISTS mdm_emp_org_assign (
    id             BIGINT       NOT NULL,               -- 内部主键（雪花生成）
    md_code        BIGINT       NOT NULL,               -- 人员主数据编码（关联 mdm_emp.md_code）
    org_code       VARCHAR(64),                         -- 组织编码（暂存 iHR departmentId 字符串形式，后续融合 md_org）
    org_name       VARCHAR(256),                        -- 组织名称（iHR departmentName）
    position_code  VARCHAR(64),                         -- 职位编码（iHR positionId）
    position_name  VARCHAR(256),                        -- 职位名称（iHR positionName）
    assign_status  VARCHAR(32)  NOT NULL,               -- 任职状态：PENDING_ONBOARD-待入职 PROBATION-试用 ACTIVE-正式 ENDED-已结束
    principal_flag CHAR(1)      NOT NULL DEFAULT 'Y',   -- 主任职标记：Y-是（当前单任职来源，恒 Y）
    valid_from     DATE,                                -- 生效起始日（入职日期）
    valid_to       DATE,                                -- 生效截止日（NULL 表示当前有效）
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted     SMALLINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_mdm_emp_org_assign PRIMARY KEY (id)
);

COMMENT ON TABLE  mdm_emp_org_assign              IS '人员任职记录表';
COMMENT ON COLUMN mdm_emp_org_assign.id           IS '内部主键';
COMMENT ON COLUMN mdm_emp_org_assign.md_code      IS '人员主数据编码';
COMMENT ON COLUMN mdm_emp_org_assign.org_code     IS '组织编码';
COMMENT ON COLUMN mdm_emp_org_assign.org_name     IS '组织名称';
COMMENT ON COLUMN mdm_emp_org_assign.position_code IS '职位编码';
COMMENT ON COLUMN mdm_emp_org_assign.position_name IS '职位名称';
COMMENT ON COLUMN mdm_emp_org_assign.assign_status IS '任职状态：PENDING_ONBOARD-待入职 PROBATION-试用 ACTIVE-正式 ENDED-已结束';
COMMENT ON COLUMN mdm_emp_org_assign.principal_flag IS '主任职标记：Y-是';
COMMENT ON COLUMN mdm_emp_org_assign.valid_from   IS '生效起始日';
COMMENT ON COLUMN mdm_emp_org_assign.valid_to     IS '生效截止日';
COMMENT ON COLUMN mdm_emp_org_assign.created_at   IS '记录创建时间';
COMMENT ON COLUMN mdm_emp_org_assign.updated_at   IS '记录更新时间';
COMMENT ON COLUMN mdm_emp_org_assign.is_deleted   IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_mdm_emp_org_assign_md_code
    ON mdm_emp_org_assign(md_code);

CREATE INDEX IF NOT EXISTS idx_mdm_emp_org_assign_active
    ON mdm_emp_org_assign(md_code) WHERE valid_to IS NULL AND is_deleted = 0;

-- ---------- 5. mdm_emp_event 事件层：人员变更事件 ----------
CREATE TABLE IF NOT EXISTS mdm_emp_event (
    id           BIGINT       NOT NULL,                 -- 内部主键（雪花生成）
    event_id     VARCHAR(64)  NOT NULL,                 -- 事件幂等键 SHA-256(mdCode:eventType:rowHash)（UNIQUE）
    event_type   VARCHAR(32)  NOT NULL,                 -- 事件类型：HIRE-入职 LEAVE-离职 REHIRE-再入职 TRANSFER-调岗 UPDATE-属性更新
    change_level VARCHAR(16)  NOT NULL,                 -- 变更级别：HIGH-入离职 NORMAL-调岗/属性更新
    md_code      BIGINT       NOT NULL,                 -- 人员主数据编码
    emp_no       VARCHAR(64),                           -- 事件发生时工号（快照）
    payload      JSONB,                                 -- 事件明细（任职、部门、职位、日期、批次号等）
    etl_batch_no VARCHAR(64),                           -- 产生本事件的 ETL 批次号
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- 处理状态：PENDING-待消费
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted   SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_mdm_emp_event PRIMARY KEY (id),
    CONSTRAINT uk_mdm_emp_event_event_id UNIQUE (event_id)
);

COMMENT ON TABLE  mdm_emp_event            IS '人员主数据变更事件表';
COMMENT ON COLUMN mdm_emp_event.id         IS '内部主键';
COMMENT ON COLUMN mdm_emp_event.event_id   IS '事件幂等键SHA-256';
COMMENT ON COLUMN mdm_emp_event.event_type IS '事件类型：HIRE-入职 LEAVE-离职 REHIRE-再入职 TRANSFER-调岗 UPDATE-属性更新';
COMMENT ON COLUMN mdm_emp_event.change_level IS '变更级别：HIGH-入离职 NORMAL-调岗/属性更新';
COMMENT ON COLUMN mdm_emp_event.md_code    IS '人员主数据编码';
COMMENT ON COLUMN mdm_emp_event.emp_no     IS '事件发生时工号';
COMMENT ON COLUMN mdm_emp_event.payload    IS '事件明细';
COMMENT ON COLUMN mdm_emp_event.etl_batch_no IS '产生本事件的ETL批次号';
COMMENT ON COLUMN mdm_emp_event.status     IS '处理状态：PENDING-待消费';
COMMENT ON COLUMN mdm_emp_event.created_at IS '记录创建时间';
COMMENT ON COLUMN mdm_emp_event.updated_at IS '记录更新时间';
COMMENT ON COLUMN mdm_emp_event.is_deleted IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_mdm_emp_event_status
    ON mdm_emp_event(status) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_mdm_emp_event_md_code
    ON mdm_emp_event(md_code);
