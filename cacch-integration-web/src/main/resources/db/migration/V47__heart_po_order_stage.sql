-- =============================================
-- V47: Heart 客户 PO 订单阶段数据表（REQ-HEART-001）
--   用途：按台账「5. SR」列结构存储 PO 完整订单的阶段数据，
--        最终提交 CRM 时从此表取数组装 contractAdd 报文，
--        并把请求/响应快照落库用于溯源。
--   三张表：
--     t_integration_crm_heart_po          PO 级阶段数据（表头 + 汇总 + 流程状态）
--     t_integration_crm_heart_po_item     PO 明细行（一产品一行，对应台账每行）
--     t_integration_crm_heart_order_submit 提交记录（每次提交一条，含请求/响应快照）
--   幂等键：po_no（一个 PO 一条主记录）；明细 (po_id, line_no)
--   阶段状态：MAIL_RECEIVED → PARSED → VALIDATED → LEDGER_WRITTEN → FILE_UPLOADED
--            → CUSTOMER_RESOLVED → NCC_BALANCE_FETCHED → SERIAL_NUMBERED
--            → ORDER_CREATED → DONE；异常：MANUAL_REVIEW / FAILED
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_crm_heart_po (
    id                        BIGINT        NOT NULL,
    po_no                     VARCHAR(64)   NOT NULL,
    cac_po_no                 VARCHAR(64),
    customer_code             VARCHAR(64),
    customer_name             VARCHAR(255),
    trade_term                VARCHAR(32),
    destination_country       VARCHAR(64),
    port_of_loading           VARCHAR(64),
    port_of_discharge         VARCHAR(64),
    port_object_id            VARCHAR(64),
    shipping_line             VARCHAR(64),
    container_type            VARCHAR(32),
    packing_type              VARCHAR(32),
    etd                       DATE,
    remark                    VARCHAR(500),
    total_cartons             NUMERIC(18, 3),
    total_liters              NUMERIC(18, 3),
    total_volume              NUMERIC(18, 4),
    total_amount              NUMERIC(18, 4),
    total_cost                NUMERIC(18, 4),
    total_gross_profit        NUMERIC(18, 4),
    gross_margin_rate         NUMERIC(10, 6),
    settlement_unit_price     NUMERIC(18, 4),
    sa_aux                    VARCHAR(255),
    sa_no                     VARCHAR(64),
    usd_gross_profit          NUMERIC(18, 4),
    usd_assess_price          NUMERIC(18, 4),
    currency                  VARCHAR(8)    NOT NULL DEFAULT 'CNY',
    mail_id                   BIGINT,
    attachment_url            VARCHAR(1000),
    parse_confidence          NUMERIC(5, 4),
    parse_payload             JSONB,
    process_status            VARCHAR(32)   NOT NULL DEFAULT 'MAIL_RECEIVED',
    crm_payload               JSONB,
    submit_count              INT           NOT NULL DEFAULT 0,
    last_submit_at            TIMESTAMP,
    fail_reason               VARCHAR(1000),
    retry_count               INT           NOT NULL DEFAULT 0,
    version                   INT           NOT NULL DEFAULT 0,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                SMALLINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_crm_heart_po PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_crm_heart_po IS 'Heart客户PO订单数据主表';
COMMENT ON COLUMN t_integration_crm_heart_po.id                    IS '内部主键';
COMMENT ON COLUMN t_integration_crm_heart_po.po_no                 IS 'PO号';
COMMENT ON COLUMN t_integration_crm_heart_po.cac_po_no             IS 'CAC PO';
COMMENT ON COLUMN t_integration_crm_heart_po.customer_code         IS 'CRM客户编码';
COMMENT ON COLUMN t_integration_crm_heart_po.customer_name         IS '客户名称';
COMMENT ON COLUMN t_integration_crm_heart_po.trade_term            IS '贸易条款';
COMMENT ON COLUMN t_integration_crm_heart_po.destination_country   IS '目的国';
COMMENT ON COLUMN t_integration_crm_heart_po.port_of_loading       IS '启运港';
COMMENT ON COLUMN t_integration_crm_heart_po.port_of_discharge     IS '目的港';
COMMENT ON COLUMN t_integration_crm_heart_po.port_object_id        IS 'CRM港口对象ID';
COMMENT ON COLUMN t_integration_crm_heart_po.shipping_line         IS '船公司';
COMMENT ON COLUMN t_integration_crm_heart_po.container_type        IS '大小柜';
COMMENT ON COLUMN t_integration_crm_heart_po.packing_type          IS '打托方式';
COMMENT ON COLUMN t_integration_crm_heart_po.etd                   IS 'ETD预计开船日期';
COMMENT ON COLUMN t_integration_crm_heart_po.remark                IS '备注';
COMMENT ON COLUMN t_integration_crm_heart_po.total_cartons         IS '箱数合计';
COMMENT ON COLUMN t_integration_crm_heart_po.total_liters          IS 'L合计';
COMMENT ON COLUMN t_integration_crm_heart_po.total_volume          IS '体积合计';
COMMENT ON COLUMN t_integration_crm_heart_po.total_amount          IS '总价合计';
COMMENT ON COLUMN t_integration_crm_heart_po.total_cost            IS '费用总计合计';
COMMENT ON COLUMN t_integration_crm_heart_po.total_gross_profit    IS '毛利合计';
COMMENT ON COLUMN t_integration_crm_heart_po.gross_margin_rate     IS '毛利率';
COMMENT ON COLUMN t_integration_crm_heart_po.settlement_unit_price IS '结算单价';
COMMENT ON COLUMN t_integration_crm_heart_po.sa_aux                IS 'SA辅助';
COMMENT ON COLUMN t_integration_crm_heart_po.sa_no                 IS 'SA NO.';
COMMENT ON COLUMN t_integration_crm_heart_po.usd_gross_profit      IS 'USD毛利';
COMMENT ON COLUMN t_integration_crm_heart_po.usd_assess_price      IS 'USD考核价';
COMMENT ON COLUMN t_integration_crm_heart_po.currency              IS '币种，默认CNY';
COMMENT ON COLUMN t_integration_crm_heart_po.mail_id               IS '来源邮件记录ID';
COMMENT ON COLUMN t_integration_crm_heart_po.attachment_url        IS 'CRM可达的PO附件URL';
COMMENT ON COLUMN t_integration_crm_heart_po.parse_confidence      IS 'AI解析置信度';
COMMENT ON COLUMN t_integration_crm_heart_po.parse_payload         IS 'AI解析原始JSON';
COMMENT ON COLUMN t_integration_crm_heart_po.process_status        IS '阶段状态：MAIL_RECEIVED/PARSED/VALIDATED/LEDGER_WRITTEN/FILE_UPLOADED/CUSTOMER_RESOLVED/NCC_BALANCE_FETCHED/SERIAL_NUMBERED/ORDER_CREATED/DONE/MANUAL_REVIEW/FAILED';
COMMENT ON COLUMN t_integration_crm_heart_po.crm_payload           IS '最终提交CRM的contractAdd请求报文快照';
COMMENT ON COLUMN t_integration_crm_heart_po.submit_count          IS '累计提交CRM次数';
COMMENT ON COLUMN t_integration_crm_heart_po.last_submit_at        IS '最近一次提交CRM时间';
COMMENT ON COLUMN t_integration_crm_heart_po.fail_reason           IS '最近失败/转人工原因';
COMMENT ON COLUMN t_integration_crm_heart_po.retry_count           IS '累计重试次数';
COMMENT ON COLUMN t_integration_crm_heart_po.version               IS '乐观锁版本号';
COMMENT ON COLUMN t_integration_crm_heart_po.created_at            IS '记录创建时间';
COMMENT ON COLUMN t_integration_crm_heart_po.updated_at            IS '记录更新时间';
COMMENT ON COLUMN t_integration_crm_heart_po.is_deleted            IS '逻辑删除：0-正常 1-删除';

-- 幂等键：一个PO一条主记录
CREATE UNIQUE INDEX IF NOT EXISTS uk_heart_po_po_no
    ON t_integration_crm_heart_po (po_no) WHERE is_deleted = 0;

-- 阶段状态捞取（轮询/补偿任务）
CREATE INDEX IF NOT EXISTS idx_heart_po_process_status
    ON t_integration_crm_heart_po (process_status, updated_at) WHERE is_deleted = 0;

-- 按CAC PO反查
CREATE INDEX IF NOT EXISTS idx_heart_po_cac_po_no
    ON t_integration_crm_heart_po (cac_po_no) WHERE is_deleted = 0;

CREATE TABLE IF NOT EXISTS t_integration_crm_heart_po_item (
    id                            BIGINT        NOT NULL,
    po_id                         BIGINT        NOT NULL,
    line_no                       INT           NOT NULL,
    product_name                  VARCHAR(64),
    spec                          VARCHAR(64),
    pd_code                       VARCHAR(32),
    product_id                    VARCHAR(64),
    carton_count                  NUMERIC(18, 3),
    liters                        NUMERIC(18, 3),
    unit_price                    NUMERIC(18, 4),
    amount                        NUMERIC(18, 4),
    aux_text                      VARCHAR(255),
    density                       NUMERIC(18, 4),
    net_weight                    NUMERIC(18, 4),
    net_weight_with_carton        NUMERIC(18, 4),
    volume                        NUMERIC(18, 4),
    carton_gross_weight           NUMERIC(18, 4),
    gross_weight                  NUMERIC(18, 4),
    empty_carton_sets             NUMERIC(18, 3),
    gross_weight_with_extra_carton NUMERIC(18, 4),
    cost_price                    NUMERIC(18, 4),
    no_pallet_unit_price          NUMERIC(18, 4),
    package_fee                   NUMERIC(18, 4),
    credit_insurance_fee          NUMERIC(18, 4),
    other_fee                     NUMERIC(18, 4),
    cost_total                    NUMERIC(18, 4),
    gross_profit                  NUMERIC(18, 4),
    gross_margin_rate             NUMERIC(10, 6),
    remark                        VARCHAR(500),
    data_source                   VARCHAR(16),
    created_at                    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                    SMALLINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_crm_heart_po_item PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_crm_heart_po_item IS 'Heart客户PO订单明细行表';
COMMENT ON COLUMN t_integration_crm_heart_po_item.id                             IS '内部主键';
COMMENT ON COLUMN t_integration_crm_heart_po_item.po_id                          IS '订单主表ID';
COMMENT ON COLUMN t_integration_crm_heart_po_item.line_no                        IS '行号';
COMMENT ON COLUMN t_integration_crm_heart_po_item.product_name                  IS '产品名称';
COMMENT ON COLUMN t_integration_crm_heart_po_item.spec                          IS '规格';
COMMENT ON COLUMN t_integration_crm_heart_po_item.pd_code                       IS 'CRM物料编码';
COMMENT ON COLUMN t_integration_crm_heart_po_item.product_id                    IS 'CRM产品对象ID';
COMMENT ON COLUMN t_integration_crm_heart_po_item.carton_count                  IS '箱数';
COMMENT ON COLUMN t_integration_crm_heart_po_item.liters                        IS 'L';
COMMENT ON COLUMN t_integration_crm_heart_po_item.unit_price                    IS '单价';
COMMENT ON COLUMN t_integration_crm_heart_po_item.amount                        IS '总价';
COMMENT ON COLUMN t_integration_crm_heart_po_item.aux_text                      IS '辅助';
COMMENT ON COLUMN t_integration_crm_heart_po_item.density                       IS '密度';
COMMENT ON COLUMN t_integration_crm_heart_po_item.net_weight                    IS '净重';
COMMENT ON COLUMN t_integration_crm_heart_po_item.net_weight_with_carton        IS '净重+纸箱';
COMMENT ON COLUMN t_integration_crm_heart_po_item.volume                        IS '体积';
COMMENT ON COLUMN t_integration_crm_heart_po_item.carton_gross_weight           IS '单箱毛重';
COMMENT ON COLUMN t_integration_crm_heart_po_item.gross_weight                  IS '毛重';
COMMENT ON COLUMN t_integration_crm_heart_po_item.empty_carton_sets             IS '空箱套数';
COMMENT ON COLUMN t_integration_crm_heart_po_item.gross_weight_with_extra_carton IS '毛重+额外纸箱';
COMMENT ON COLUMN t_integration_crm_heart_po_item.cost_price                    IS '成本价格（CNY/L）';
COMMENT ON COLUMN t_integration_crm_heart_po_item.no_pallet_unit_price          IS '不打托单价';
COMMENT ON COLUMN t_integration_crm_heart_po_item.package_fee                   IS '包干费（元）';
COMMENT ON COLUMN t_integration_crm_heart_po_item.credit_insurance_fee          IS '中信保（元）';
COMMENT ON COLUMN t_integration_crm_heart_po_item.other_fee                     IS '其它杂费（元）';
COMMENT ON COLUMN t_integration_crm_heart_po_item.cost_total                    IS '费用总计（元）';
COMMENT ON COLUMN t_integration_crm_heart_po_item.gross_profit                  IS '毛利';
COMMENT ON COLUMN t_integration_crm_heart_po_item.gross_margin_rate             IS '毛利率';
COMMENT ON COLUMN t_integration_crm_heart_po_item.remark                        IS '备注';
COMMENT ON COLUMN t_integration_crm_heart_po_item.data_source                   IS '数据来源：AI-模型解析/MANUAL-人工/COMPUTED-公式计算';
COMMENT ON COLUMN t_integration_crm_heart_po_item.created_at                    IS '记录创建时间';
COMMENT ON COLUMN t_integration_crm_heart_po_item.updated_at                    IS '记录更新时间';
COMMENT ON COLUMN t_integration_crm_heart_po_item.is_deleted                    IS '逻辑删除：0-正常 1-删除';

CREATE UNIQUE INDEX IF NOT EXISTS uk_heart_po_item_po_line
    ON t_integration_crm_heart_po_item (po_id, line_no) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_heart_po_item_po_id
    ON t_integration_crm_heart_po_item (po_id) WHERE is_deleted = 0;

CREATE TABLE IF NOT EXISTS t_integration_crm_heart_order_submit (
    id                  BIGINT        NOT NULL,
    po_id               BIGINT        NOT NULL,
    attempt_no          INT           NOT NULL,
    submit_status       VARCHAR(16)   NOT NULL,
    crm_contract_id     VARCHAR(64),
    crm_contract_no     VARCHAR(64),
    crm_serial_no       VARCHAR(64),
    return_code         VARCHAR(32),
    return_msg          VARCHAR(1000),
    http_status         INT,
    error_message       VARCHAR(1000),
    request_payload     JSONB,
    response_payload    JSONB,
    duration_ms         INT,
    trace_id            VARCHAR(64),
    submitted_at        TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_crm_heart_order_submit PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_crm_heart_order_submit IS 'Heart客户PO提交CRM订单记录表';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.id               IS '内部主键';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.po_id            IS '订单主表ID';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.attempt_no       IS '提交序号';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.submit_status    IS '提交结果：SUCCESS-成功/FAILED-失败';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.crm_contract_id  IS 'CRM订单数据ID';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.crm_contract_no  IS 'CRM订单编号';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.crm_serial_no    IS 'CRM取号流水号';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.return_code      IS 'CRM返回码return_code';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.return_msg       IS 'CRM返回描述return_msg';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.http_status      IS 'HTTP响应状态码';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.error_message    IS '调用异常信息';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.request_payload  IS '本次提交的完整请求报文';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.response_payload IS '本次提交的完整响应报文';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.duration_ms      IS '调用耗时（毫秒）';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.trace_id         IS '链路追踪TraceId';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.submitted_at     IS '提交时间';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.created_at       IS '记录创建时间';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.updated_at       IS '记录更新时间';
COMMENT ON COLUMN t_integration_crm_heart_order_submit.is_deleted       IS '逻辑删除：0-正常 1-删除';

CREATE UNIQUE INDEX IF NOT EXISTS uk_heart_order_submit_po_attempt
    ON t_integration_crm_heart_order_submit (po_id, attempt_no) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_heart_order_submit_po_id
    ON t_integration_crm_heart_order_submit (po_id, submitted_at) WHERE is_deleted = 0;
