-- =============================================
-- V33: 客如云订单详情落库（菜品明细 + 支付明细）
--   数据来源：POST /open/standard/order/queryDetail
--     result.data.orderItemVoList[]   → stg_keruyun_order_item
--     result.data.openPaymentDetailVoList[] → stg_keruyun_order_payment
--   落库策略：仅同步完结订单（finish_time 非空）；
--     每单整体 delete + insert 幂等重跑，主表 detail_synced_at 记录成功时间
--   金额单位：分（BIGINT）；数量 NUMERIC(12,4)（客如云返回如 1.0000）
-- =============================================

-- 菜品明细表
CREATE TABLE IF NOT EXISTS stg_keruyun_order_item (
    id                     BIGINT        NOT NULL,
    order_id               VARCHAR(64)   NOT NULL,
    item_row_id            VARCHAR(64),
    parent_item_row_id     VARCHAR(64),
    item_id                VARCHAR(64),
    item_sku_id            VARCHAR(64),
    item_type              VARCHAR(32),
    item_code              VARCHAR(64),
    item_name              VARCHAR(128),
    big_type_name          VARCHAR(64),
    mid_type_name          VARCHAR(64),
    unit_name              VARCHAR(32),
    spec_name              VARCHAR(64),
    spec_name_concat       VARCHAR(128),
    production_dept_id     VARCHAR(64),
    sale_status_type       VARCHAR(32),
    sale_status_type_code  VARCHAR(32),
    gift_flag              BOOLEAN,
    weigh_flag             BOOLEAN,
    temp_flag              BOOLEAN,
    promo_flag             BOOLEAN,
    quantity               NUMERIC(12,4),
    item_price             BIGINT,
    sale_price             BIGINT,
    item_sale_amt          BIGINT,
    extra_fee_apportion_amt BIGINT,
    item_promo_apportion_amt BIGINT,
    item_received_amt      BIGINT,
    practice_info          VARCHAR(512),
    item_subject           VARCHAR(255),
    item_refund_time       TIMESTAMP,
    tax_rate               VARCHAR(16),
    item_actual_tax_amt    BIGINT,
    actual_tax_amt         BIGINT,
    created_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted             SMALLINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_stg_keruyun_order_item PRIMARY KEY (id)
);

COMMENT ON TABLE  stg_keruyun_order_item IS '客如云订单菜品明细表';
COMMENT ON COLUMN stg_keruyun_order_item.id                      IS '内部主键';
COMMENT ON COLUMN stg_keruyun_order_item.order_id                IS '客如云订单 ID（关联 stg_keruyun_order.order_id）';
COMMENT ON COLUMN stg_keruyun_order_item.item_row_id            IS '客如云商品行 ID（文档字段 id），订单内唯一';
COMMENT ON COLUMN stg_keruyun_order_item.parent_item_row_id     IS '父商品行 ID（套餐子商品场景，关联本表 item_row_id）';
COMMENT ON COLUMN stg_keruyun_order_item.item_id                IS '商品档案 ID（文档字段 itemId）';
COMMENT ON COLUMN stg_keruyun_order_item.item_sku_id            IS '规格商品 ID';
COMMENT ON COLUMN stg_keruyun_order_item.item_type               IS '商品类型：SINGLE_DISH/SINGLE_SET/COMBO/CUSTOMER_SET 等';
COMMENT ON COLUMN stg_keruyun_order_item.item_code               IS '商品编码';
COMMENT ON COLUMN stg_keruyun_order_item.item_name               IS '商品名称';
COMMENT ON COLUMN stg_keruyun_order_item.big_type_name           IS '商品大类名称';
COMMENT ON COLUMN stg_keruyun_order_item.mid_type_name           IS '商品中类名称';
COMMENT ON COLUMN stg_keruyun_order_item.unit_name               IS '单位名称';
COMMENT ON COLUMN stg_keruyun_order_item.spec_name               IS '规格';
COMMENT ON COLUMN stg_keruyun_order_item.spec_name_concat        IS '规格名称全称';
COMMENT ON COLUMN stg_keruyun_order_item.production_dept_id     IS '出品部门 ID';
COMMENT ON COLUMN stg_keruyun_order_item.sale_status_type        IS '商品售卖状态：NORMAL-销售/DISCARD-退菜';
COMMENT ON COLUMN stg_keruyun_order_item.sale_status_type_code   IS '商品售卖状态编码：NORMAL/DISCARD';
COMMENT ON COLUMN stg_keruyun_order_item.gift_flag                IS '是否赠送：true-赠菜';
COMMENT ON COLUMN stg_keruyun_order_item.weigh_flag              IS '是否称重商品';
COMMENT ON COLUMN stg_keruyun_order_item.temp_flag               IS '是否临时菜';
COMMENT ON COLUMN stg_keruyun_order_item.promo_flag              IS '是否优惠菜';
COMMENT ON COLUMN stg_keruyun_order_item.quantity                IS '销售/退菜/赠菜数量';
COMMENT ON COLUMN stg_keruyun_order_item.item_price              IS '商品原始单价（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_item.sale_price              IS '商品售价（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_item.item_sale_amt           IS '商品销售金额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_item.extra_fee_apportion_amt IS '服务费分摊金额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_item.item_promo_apportion_amt IS '商品优惠分摊（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_item.item_received_amt      IS '商品收入（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_item.practice_info          IS '做法信息（扁平化，格式：做法名:金额(分)，分号分隔）';
COMMENT ON COLUMN stg_keruyun_order_item.item_subject           IS '商品备注';
COMMENT ON COLUMN stg_keruyun_order_item.item_refund_time       IS '菜品退款时间（仅外卖退款有值）';
COMMENT ON COLUMN stg_keruyun_order_item.tax_rate                IS '商品税率（如 1.53%）';
COMMENT ON COLUMN stg_keruyun_order_item.item_actual_tax_amt    IS '单品税额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_item.actual_tax_amt         IS '菜品行税额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_item.created_at             IS '记录创建时间';
COMMENT ON COLUMN stg_keruyun_order_item.updated_at             IS '记录更新时间';
COMMENT ON COLUMN stg_keruyun_order_item.is_deleted             IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_stg_keruyun_order_item_order
    ON stg_keruyun_order_item(order_id) WHERE is_deleted = 0;

-- 支付明细表
CREATE TABLE IF NOT EXISTS stg_keruyun_order_payment (
    id                      BIGINT        NOT NULL,
    order_id                VARCHAR(64)   NOT NULL,
    pay_detail_no           VARCHAR(64)   NOT NULL,
    pay_method_id           BIGINT,
    pay_method_name         VARCHAR(64),
    pay_detail_status       VARCHAR(32),
    face_amt                BIGINT,
    pay_amt                 BIGINT,
    actual_pay_amt          BIGINT,
    actual_receive_amt      BIGINT,
    shop_promo_amt          BIGINT,
    pay_promo_amt           BIGINT,
    platform_promo_amt      BIGINT,
    platform_service_amt    BIGINT,
    coupon_cnt              VARCHAR(16),
    coupon_name             VARCHAR(255),
    coupon_code             VARCHAR(255),
    coupon_type             INTEGER,
    coupon_reconcile_flag   BOOLEAN,
    third_voucher_item_id   VARCHAR(64),
    pay_detail_start_time   TIMESTAMP,
    pay_detail_end_time     TIMESTAMP,
    operator_name           VARCHAR(64),
    credit_member_id        VARCHAR(64),
    credit_member_name      VARCHAR(64),
    credit_member_phone_no  VARCHAR(32),
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_stg_keruyun_order_payment PRIMARY KEY (id),
    CONSTRAINT uk_stg_keruyun_order_payment_no UNIQUE (order_id, pay_detail_no)
);

COMMENT ON TABLE  stg_keruyun_order_payment IS '客如云订单支付明细表';
COMMENT ON COLUMN stg_keruyun_order_payment.id                     IS '内部主键';
COMMENT ON COLUMN stg_keruyun_order_payment.order_id               IS '客如云订单 ID（关联 stg_keruyun_order.order_id）';
COMMENT ON COLUMN stg_keruyun_order_payment.pay_detail_no          IS '客如云支付单号，订单内唯一';
COMMENT ON COLUMN stg_keruyun_order_payment.pay_method_id          IS '支付方式 ID：-3-现金/-4-银行卡/-129-扫码支付/-130-收银码/-5-微信/-6-支付宝/-37-云闪付/-1-会员卡/-15-实体卡/-20-匿名卡/-127-储值补录/-128-挂账/-24-美团团购券/-36-口碑团购券/0-抵用券';
COMMENT ON COLUMN stg_keruyun_order_payment.pay_method_name        IS '支付方式名称';
COMMENT ON COLUMN stg_keruyun_order_payment.pay_detail_status      IS '支付状态：PAY_SUCCESS 等';
COMMENT ON COLUMN stg_keruyun_order_payment.face_amt               IS '面额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_payment.pay_amt               IS '支付金额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_payment.actual_pay_amt        IS '实际支付金额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_payment.actual_receive_amt    IS '商户实收（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_payment.shop_promo_amt         IS '商户优惠金额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_payment.pay_promo_amt          IS '支付优惠（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_payment.platform_promo_amt    IS '平台优惠金额（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_payment.platform_service_amt   IS '平台抽佣/服务费（单位：分）';
COMMENT ON COLUMN stg_keruyun_order_payment.coupon_cnt             IS '券数量';
COMMENT ON COLUMN stg_keruyun_order_payment.coupon_name            IS '券名称';
COMMENT ON COLUMN stg_keruyun_order_payment.coupon_code            IS '券码，多张券以英文逗号分隔';
COMMENT ON COLUMN stg_keruyun_order_payment.coupon_type            IS '团购券类型：1-代金券/2-套餐券/3-次卡/4-阶梯次卡';
COMMENT ON COLUMN stg_keruyun_order_payment.coupon_reconcile_flag IS '团购券是否已对账';
COMMENT ON COLUMN stg_keruyun_order_payment.third_voucher_item_id  IS '券模板 ID（平台券项目 ID）';
COMMENT ON COLUMN stg_keruyun_order_payment.pay_detail_start_time  IS '支付创建/支出开始时间';
COMMENT ON COLUMN stg_keruyun_order_payment.pay_detail_end_time    IS '支付/退款完成时间';
COMMENT ON COLUMN stg_keruyun_order_payment.operator_name          IS '操作人姓名/收银员';
COMMENT ON COLUMN stg_keruyun_order_payment.credit_member_id       IS '挂账人 ID（payMethodId=-128 时有值）';
COMMENT ON COLUMN stg_keruyun_order_payment.credit_member_name     IS '挂账人名称';
COMMENT ON COLUMN stg_keruyun_order_payment.credit_member_phone_no IS '挂账人手机号';
COMMENT ON COLUMN stg_keruyun_order_payment.created_at             IS '记录创建时间';
COMMENT ON COLUMN stg_keruyun_order_payment.updated_at             IS '记录更新时间';
COMMENT ON COLUMN stg_keruyun_order_payment.is_deleted             IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_stg_keruyun_order_payment_order
    ON stg_keruyun_order_payment(order_id) WHERE is_deleted = 0;

-- 主表增加详情同步成功时间（完结订单待同步队列依据）
ALTER TABLE stg_keruyun_order
    ADD COLUMN IF NOT EXISTS detail_synced_at TIMESTAMP;

COMMENT ON COLUMN stg_keruyun_order.detail_synced_at IS '订单详情（菜品/支付）最近一次同步成功时间，NULL 表示未同步';

CREATE INDEX IF NOT EXISTS idx_stg_keruyun_order_detail_pending
    ON stg_keruyun_order(finish_time) WHERE is_deleted = 0 AND detail_synced_at IS NULL;
