-- =============================================
-- V10: CRM订单同步OA表单（REQ-CRM-001）
--     订单主表 + 订单明细（含OA同步状态）+ CRM↔OA人员映射
--     设计原则：结构化关键列 + JSONB 原始报文（raw_payload），文档未列字段不丢
-- =============================================

-- ---------------------------------------------
-- 1. CRM订单主表
--    来源：勤策 orderQuery；按 order_no（CRM name）判重，仅新增不更新
-- ---------------------------------------------
CREATE TABLE IF NOT EXISTS t_integration_crm_order (
    id                      BIGINT        NOT NULL,
    crm_order_id            VARCHAR(64)   NOT NULL,
    order_no                VARCHAR(128)  NOT NULL,
    customer_id             VARCHAR(64),
    customer_name           VARCHAR(255),
    owner_id                VARCHAR(64),
    owner_name              VARCHAR(128),
    currency_type           VARCHAR(64),
    order_total_amount      VARCHAR(64),
    crm_create_time         TIMESTAMP,
    crm_modify_time         TIMESTAMP,
    detail_fetch_status     VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    detail_fetch_error      TEXT,
    detail_fetch_time       TIMESTAMP,
    raw_payload             JSONB         NOT NULL,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_crm_order PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_crm_order IS 'CRM订单主表——勤策orderQuery入库；结构化关键列+raw_payload完整原始报文';
COMMENT ON COLUMN t_integration_crm_order.id                  IS '主键（雪花）';
COMMENT ON COLUMN t_integration_crm_order.crm_order_id        IS 'CRM订单内部ID（接口字段id）';
COMMENT ON COLUMN t_integration_crm_order.order_no            IS 'CRM订单编号（接口字段name），业务去重键';
COMMENT ON COLUMN t_integration_crm_order.customer_id         IS '客户ID（从customer.id展开，便于查询）';
COMMENT ON COLUMN t_integration_crm_order.customer_name       IS '客户名称（从customer.name展开，映射OA field0008）';
COMMENT ON COLUMN t_integration_crm_order.owner_id            IS '负责人员工ID（从owner.id展开，人员映射入口）';
COMMENT ON COLUMN t_integration_crm_order.owner_name          IS '负责人姓名（从owner.name展开）';
COMMENT ON COLUMN t_integration_crm_order.currency_type       IS '币种展示值（可从currency_type.label展开，完整对象在raw_payload）';
COMMENT ON COLUMN t_integration_crm_order.order_total_amount  IS '订单总额（接口字段order_total_amount）';
COMMENT ON COLUMN t_integration_crm_order.crm_create_time     IS 'CRM创建时间（从create_time解析）';
COMMENT ON COLUMN t_integration_crm_order.crm_modify_time     IS 'CRM修改时间（从modify_time解析；采集过滤字段）';
COMMENT ON COLUMN t_integration_crm_order.detail_fetch_status IS '明细拉取状态：PENDING-待拉取 SUCCESS-已成功 FAILED-失败待补拉';
COMMENT ON COLUMN t_integration_crm_order.detail_fetch_error  IS '明细拉取失败原因';
COMMENT ON COLUMN t_integration_crm_order.detail_fetch_time   IS '最近一次明细拉取尝试时间';
COMMENT ON COLUMN t_integration_crm_order.raw_payload         IS 'orderQuery单条订单完整JSON（含文档未列字段）';
COMMENT ON COLUMN t_integration_crm_order.created_at          IS '创建时间';
COMMENT ON COLUMN t_integration_crm_order.updated_at          IS '更新时间';
COMMENT ON COLUMN t_integration_crm_order.is_deleted          IS '逻辑删除：0-正常 1-删除';

CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_order_no
    ON t_integration_crm_order (order_no) WHERE is_deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_order_crm_id
    ON t_integration_crm_order (crm_order_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_order_modify_time
    ON t_integration_crm_order (crm_modify_time) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_order_detail_fetch
    ON t_integration_crm_order (detail_fetch_status) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_order_owner_id
    ON t_integration_crm_order (owner_id) WHERE is_deleted = 0;


-- ---------------------------------------------
-- 2. CRM订单明细表（子订单）
--    来源：勤策 orderDetailQuery；按 crm_detail_id 判重，仅新增不更新
--    OA同步粒度：一条明细对应一个OA表单；同步状态挂本表
-- ---------------------------------------------
CREATE TABLE IF NOT EXISTS t_integration_crm_order_detail (
    id                      BIGINT        NOT NULL,
    order_id                BIGINT        NOT NULL,
    crm_order_id            VARCHAR(64)   NOT NULL,
    order_no                VARCHAR(128)  NOT NULL,
    crm_detail_id           VARCHAR(64)   NOT NULL,
    detail_name             VARCHAR(128),
    pd_code                 VARCHAR(128),
    pd_count                VARCHAR(64),
    actual_price            VARCHAR(64),
    material_code           VARCHAR(128),
    oa_sync_status          VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    oa_process_id           VARCHAR(128),
    oa_sync_time            TIMESTAMP,
    retry_count             INT           NOT NULL DEFAULT 0,
    last_error_msg          TEXT,
    raw_payload             JSONB         NOT NULL,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_crm_order_detail PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_crm_order_detail IS 'CRM订单明细表——orderDetailQuery入库；一条明细对应一个OA表单；含OA同步状态';
COMMENT ON COLUMN t_integration_crm_order_detail.id             IS '主键（雪花）';
COMMENT ON COLUMN t_integration_crm_order_detail.order_id       IS '关联 t_integration_crm_order.id';
COMMENT ON COLUMN t_integration_crm_order_detail.crm_order_id   IS 'CRM订单内部ID（冗余，便于排查）';
COMMENT ON COLUMN t_integration_crm_order_detail.order_no       IS 'CRM订单编号（冗余）';
COMMENT ON COLUMN t_integration_crm_order_detail.crm_detail_id  IS 'CRM明细ID（接口字段id），OA同步去重键';
COMMENT ON COLUMN t_integration_crm_order_detail.detail_name    IS '明细编号（接口字段name）';
COMMENT ON COLUMN t_integration_crm_order_detail.pd_code        IS '商品编码（零售包装判断：53开头→是）';
COMMENT ON COLUMN t_integration_crm_order_detail.pd_count       IS '商品数量（映射OA field0074）';
COMMENT ON COLUMN t_integration_crm_order_detail.actual_price   IS '实际售价（映射OA field0092）';
COMMENT ON COLUMN t_integration_crm_order_detail.material_code  IS '物料编号（可从field_Mb25P__c展开，完整值在raw_payload）';
COMMENT ON COLUMN t_integration_crm_order_detail.oa_sync_status IS 'OA同步状态：PENDING/SUCCESS/RETRY/FAILED';
COMMENT ON COLUMN t_integration_crm_order_detail.oa_process_id  IS 'OA流程实例ID（同步成功后记录）';
COMMENT ON COLUMN t_integration_crm_order_detail.oa_sync_time   IS '最近一次OA同步时间';
COMMENT ON COLUMN t_integration_crm_order_detail.retry_count    IS 'OA同步重试次数（最大3次）';
COMMENT ON COLUMN t_integration_crm_order_detail.last_error_msg IS '最近一次同步/映射失败错误信息';
COMMENT ON COLUMN t_integration_crm_order_detail.raw_payload    IS 'orderDetailQuery单条明细完整JSON（含文档未列字段）';
COMMENT ON COLUMN t_integration_crm_order_detail.created_at     IS '创建时间';
COMMENT ON COLUMN t_integration_crm_order_detail.updated_at     IS '更新时间';
COMMENT ON COLUMN t_integration_crm_order_detail.is_deleted     IS '逻辑删除：0-正常 1-删除';

CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_order_detail_crm_id
    ON t_integration_crm_order_detail (crm_detail_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_order_detail_order_id
    ON t_integration_crm_order_detail (order_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_order_detail_order_no
    ON t_integration_crm_order_detail (order_no) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_order_detail_oa_sync
    ON t_integration_crm_order_detail (oa_sync_status, retry_count) WHERE is_deleted = 0;


-- ---------------------------------------------
-- 3. CRM↔OA 人员映射表（独立能力）
--    订单OA同步时触发：owner.id → CRM emp_code → OA人员ID，并落库复用
--    允许更新（与订单仅新增不更新策略分离）
-- ---------------------------------------------
CREATE TABLE IF NOT EXISTS t_integration_crm_oa_user_mapping (
    id                      BIGINT        NOT NULL,
    crm_employee_id         VARCHAR(64)   NOT NULL,
    emp_code                VARCHAR(128),
    oa_user_id              VARCHAR(64),
    oa_login_name           VARCHAR(128),
    crm_employee_name       VARCHAR(128),
    crm_raw_payload         JSONB,
    oa_raw_payload          JSONB,
    last_mapped_at          TIMESTAMP,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_crm_oa_user_mapping PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_crm_oa_user_mapping IS 'CRM↔OA人员映射表——独立能力；结构化关键字段+两侧原始报文预留';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.id                IS '主键（雪花）';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.crm_employee_id   IS 'CRM员工ID（订单owner.id），业务唯一键';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.emp_code          IS 'CRM员工登录帐号（queryEmployee返回emp_code）';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.oa_user_id        IS 'OA人员ID（orgMembers/code返回id）；业务员与发起人共用';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.oa_login_name     IS 'OA登录名（若返回则保存，供Token loginName）';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.crm_employee_name IS 'CRM员工姓名（可选，便于排查）';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.crm_raw_payload   IS 'CRM查询员工帐号完整响应JSON';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.oa_raw_payload    IS 'OA按编码取人员完整响应JSON';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.last_mapped_at    IS '最近一次成功映射时间';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.created_at        IS '创建时间';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.updated_at        IS '更新时间';
COMMENT ON COLUMN t_integration_crm_oa_user_mapping.is_deleted        IS '逻辑删除：0-正常 1-删除';

CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_oa_user_crm_employee_id
    ON t_integration_crm_oa_user_mapping (crm_employee_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_oa_user_emp_code
    ON t_integration_crm_oa_user_mapping (emp_code) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_crm_oa_user_oa_user_id
    ON t_integration_crm_oa_user_mapping (oa_user_id) WHERE is_deleted = 0;
