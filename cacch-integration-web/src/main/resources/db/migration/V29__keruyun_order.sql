-- =============================================
-- V29: 客如云订单落库表
--   数据来源：POST /open/standard/order/queryList（result.data.list[]）
--   落库策略：order_id 为业务唯一键，先查后写实现幂等重跑
--   金额单位：分（BIGINT）
-- =============================================

CREATE TABLE IF NOT EXISTS stg_keruyun_order (
    id                  BIGINT       NOT NULL,
    brand_id            VARCHAR(64),
    shop_id             VARCHAR(64),
    order_id            VARCHAR(64)  NOT NULL,
    busi_order_no       VARCHAR(64),
    order_status        VARCHAR(32),
    order_type          VARCHAR(32),
    order_amt           BIGINT,
    promo_amt           BIGINT,
    order_received_amt  BIGINT,
    open_time           TIMESTAMP,
    finish_time         TIMESTAMP,
    third_order_no      VARCHAR(64),
    serial_no           VARCHAR(64),
    third_serial_no     VARCHAR(64),
    order_mode_tag      VARCHAR(32),
    order_mode          VARCHAR(32),
    sync_batch          VARCHAR(64),
    synced_at           TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_stg_keruyun_order PRIMARY KEY (id),
    CONSTRAINT uk_stg_keruyun_order_order_id UNIQUE (order_id)
);

COMMENT ON TABLE  stg_keruyun_order IS '客如云订单落库表（订单列表查询接口 result.data.list）';
COMMENT ON COLUMN stg_keruyun_order.id                 IS '内部主键（雪花生成）';
COMMENT ON COLUMN stg_keruyun_order.brand_id           IS '品牌 ID';
COMMENT ON COLUMN stg_keruyun_order.shop_id            IS '门店 ID';
COMMENT ON COLUMN stg_keruyun_order.order_id           IS '客如云订单 ID（业务唯一键）';
COMMENT ON COLUMN stg_keruyun_order.busi_order_no      IS '业务订单号';
COMMENT ON COLUMN stg_keruyun_order.order_status       IS '订单状态：WAIT_PROCESSED/ORDER_RECEIVED/SUCCESS/WAIT_SETTLED/SETTLED/REFUND/CLOSED/INVALID/CANCELLED/REJECTED/ANTI_SETTLED';
COMMENT ON COLUMN stg_keruyun_order.order_type         IS '订单类型：FOR_HERE/PLATFORM_TAKE_OUT/SELF_TAKE_OUT/SELF_TAKE/NO_ORDER_CASHIER/MEMBER_STORE/MEMBER_MANUAL_STORE/REPAYMENT_ORDER';
COMMENT ON COLUMN stg_keruyun_order.order_amt          IS '订单金额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order.promo_amt          IS '优惠金额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order.order_received_amt IS '订单收入/订单实收（单位：分）';
COMMENT ON COLUMN stg_keruyun_order.open_time          IS '下单时间';
COMMENT ON COLUMN stg_keruyun_order.finish_time        IS '完结时间';
COMMENT ON COLUMN stg_keruyun_order.third_order_no     IS '第三方订单号';
COMMENT ON COLUMN stg_keruyun_order.serial_no          IS '订单流水号';
COMMENT ON COLUMN stg_keruyun_order.third_serial_no    IS '第三方流水号';
COMMENT ON COLUMN stg_keruyun_order.order_mode_tag     IS '订单模式标签：REV_POSITIVE_ORDER/REV_REVERSE_ORDER/REV_RESULT_ORDER/ORG_RESULT_ORDER';
COMMENT ON COLUMN stg_keruyun_order.order_mode         IS '订单模式：ORIGINAL_ORDER/REVERSE_ORDER/HISTORY';
COMMENT ON COLUMN stg_keruyun_order.sync_batch         IS '同步批次号（便于追溯）';
COMMENT ON COLUMN stg_keruyun_order.synced_at          IS '最近一次从客如云同步成功的时间';
COMMENT ON COLUMN stg_keruyun_order.created_at         IS '记录创建时间';
COMMENT ON COLUMN stg_keruyun_order.updated_at         IS '记录更新时间';
COMMENT ON COLUMN stg_keruyun_order.is_deleted         IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_stg_keruyun_order_shop
    ON stg_keruyun_order(shop_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_stg_keruyun_order_open_time
    ON stg_keruyun_order(open_time) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_stg_keruyun_order_finish_time
    ON stg_keruyun_order(finish_time) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_stg_keruyun_order_status
    ON stg_keruyun_order(order_status) WHERE is_deleted = 0;
