-- =============================================================================
-- REQ-CRM-001 阶段一：CRM 订单采集入库 — 业务核对 SQL（PostgreSQL）
-- 表：t_integration_crm_order / t_integration_crm_order_detail
-- 说明：按需替换 '你的订单编号' / '你的CRM订单ID' 后执行；SELECT 列均带中文别名
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 采集总览（按明细拉取状态）
-- -----------------------------------------------------------------------------
SELECT detail_fetch_status AS "明细拉取状态",
       COUNT(*)            AS "订单数量"
FROM t_integration_crm_order
WHERE is_deleted = 0
GROUP BY detail_fetch_status
ORDER BY detail_fetch_status;
-- 明细拉取状态：PENDING-待拉取 / SUCCESS-已成功 / FAILED-失败待补拉


-- -----------------------------------------------------------------------------
-- 2. 今日入库订单（按本系统 created_at）
-- -----------------------------------------------------------------------------
SELECT id                     AS "主键ID",
       order_no               AS "订单编号",
       crm_order_id           AS "CRM订单ID",
       customer_name          AS "客户名称",
       owner_id               AS "负责人员工ID",
       owner_name             AS "负责人姓名",
       order_total_amount     AS "订单总额",
       crm_modify_time        AS "CRM修改时间",
       detail_fetch_status    AS "明细拉取状态",
       detail_fetch_error     AS "明细拉取失败原因",
       detail_fetch_time      AS "明细最近拉取时间",
       created_at             AS "入库时间"
FROM t_integration_crm_order
WHERE is_deleted = 0
  AND created_at >= CURRENT_DATE
  AND created_at < CURRENT_DATE + INTERVAL '1 day'
ORDER BY created_at DESC;


-- -----------------------------------------------------------------------------
-- 3. 按 CRM modify_time 查当天（对齐采集默认时间窗）
-- -----------------------------------------------------------------------------
SELECT id                     AS "主键ID",
       order_no               AS "订单编号",
       crm_order_id           AS "CRM订单ID",
       customer_name          AS "客户名称",
       owner_name             AS "负责人姓名",
       crm_modify_time        AS "CRM修改时间",
       detail_fetch_status    AS "明细拉取状态",
       created_at             AS "入库时间"
FROM t_integration_crm_order
WHERE is_deleted = 0
  AND crm_modify_time >= CURRENT_DATE
  AND crm_modify_time < CURRENT_DATE + INTERVAL '1 day'
ORDER BY crm_modify_time DESC NULLS LAST;


-- -----------------------------------------------------------------------------
-- 4. 待补拉 / 失败订单（定时任务补拉对象）
-- -----------------------------------------------------------------------------
SELECT id                     AS "主键ID",
       order_no               AS "订单编号",
       crm_order_id           AS "CRM订单ID",
       detail_fetch_status    AS "明细拉取状态",
       detail_fetch_error     AS "明细拉取失败原因",
       detail_fetch_time      AS "明细最近拉取时间",
       created_at             AS "入库时间",
       updated_at             AS "更新时间"
FROM t_integration_crm_order
WHERE is_deleted = 0
  AND detail_fetch_status IN ('PENDING', 'FAILED')
ORDER BY updated_at ASC
LIMIT 200;


-- -----------------------------------------------------------------------------
-- 5. 按订单编号查主表 + 明细汇总
--    替换：'你的订单编号'
-- -----------------------------------------------------------------------------
SELECT o.id                   AS "主键ID",
       o.order_no             AS "订单编号",
       o.crm_order_id         AS "CRM订单ID",
       o.customer_name        AS "客户名称",
       o.owner_id             AS "负责人员工ID",
       o.owner_name           AS "负责人姓名",
       o.currency_type        AS "币种",
       o.order_total_amount   AS "订单总额",
       o.crm_create_time      AS "CRM创建时间",
       o.crm_modify_time      AS "CRM修改时间",
       o.detail_fetch_status  AS "明细拉取状态",
       o.detail_fetch_error   AS "明细拉取失败原因",
       o.detail_fetch_time    AS "明细最近拉取时间",
       o.created_at           AS "入库时间",
       COUNT(d.id)            AS "明细行数",
       COUNT(d.id) FILTER (WHERE d.oa_sync_status = 'PENDING') AS "待OA同步明细数"
FROM t_integration_crm_order o
LEFT JOIN t_integration_crm_order_detail d
       ON d.order_id = o.id AND d.is_deleted = 0
WHERE o.is_deleted = 0
  AND o.order_no = '你的订单编号'   -- TODO 替换
GROUP BY o.id;


-- -----------------------------------------------------------------------------
-- 6. 按订单编号查明细清单
-- -----------------------------------------------------------------------------
SELECT d.id                   AS "主键ID",
       d.order_no             AS "订单编号",
       d.crm_order_id         AS "CRM订单ID",
       d.crm_detail_id        AS "CRM明细ID",
       d.detail_name          AS "明细编号",
       d.pd_code              AS "商品编码",
       d.pd_count             AS "商品数量",
       d.actual_price         AS "实际售价",
       d.material_code        AS "物料编号",
       d.oa_sync_status       AS "OA同步状态",
       d.retry_count          AS "OA重试次数",
       d.last_error_msg       AS "最近错误信息",
       d.created_at           AS "入库时间"
FROM t_integration_crm_order_detail d
WHERE d.is_deleted = 0
  AND d.order_no = '你的订单编号'   -- TODO 替换
ORDER BY d.id;
-- OA同步状态：PENDING-待同步 / SUCCESS-已成功 / RETRY-重试中 / FAILED-已失败


-- -----------------------------------------------------------------------------
-- 7. 按 CRM 订单内部 ID 核对
-- -----------------------------------------------------------------------------
SELECT o.order_no             AS "订单编号",
       o.crm_order_id         AS "CRM订单ID",
       o.detail_fetch_status  AS "明细拉取状态",
       d.crm_detail_id        AS "CRM明细ID",
       d.pd_code              AS "商品编码",
       d.pd_count             AS "商品数量",
       d.actual_price         AS "实际售价",
       d.oa_sync_status       AS "OA同步状态"
FROM t_integration_crm_order o
LEFT JOIN t_integration_crm_order_detail d
       ON d.order_id = o.id AND d.is_deleted = 0
WHERE o.is_deleted = 0
  AND o.crm_order_id = '你的CRM订单ID'   -- TODO 替换
ORDER BY d.id;


-- -----------------------------------------------------------------------------
-- 8. 一致性：标记 SUCCESS 但无明细行（异常）
-- -----------------------------------------------------------------------------
SELECT o.id                   AS "主键ID",
       o.order_no             AS "订单编号",
       o.crm_order_id         AS "CRM订单ID",
       o.detail_fetch_status  AS "明细拉取状态",
       o.detail_fetch_time    AS "明细最近拉取时间",
       o.created_at           AS "入库时间"
FROM t_integration_crm_order o
WHERE o.is_deleted = 0
  AND o.detail_fetch_status = 'SUCCESS'
  AND NOT EXISTS (
        SELECT 1
        FROM t_integration_crm_order_detail d
        WHERE d.order_id = o.id
          AND d.is_deleted = 0
    )
ORDER BY o.created_at DESC;


-- -----------------------------------------------------------------------------
-- 9. 一致性：有明细但主表仍为 PENDING/FAILED（异常）
-- -----------------------------------------------------------------------------
SELECT o.id                   AS "主键ID",
       o.order_no             AS "订单编号",
       o.detail_fetch_status  AS "明细拉取状态",
       o.detail_fetch_error   AS "明细拉取失败原因",
       COUNT(d.id)            AS "明细行数"
FROM t_integration_crm_order o
JOIN t_integration_crm_order_detail d
  ON d.order_id = o.id AND d.is_deleted = 0
WHERE o.is_deleted = 0
  AND o.detail_fetch_status IN ('PENDING', 'FAILED')
GROUP BY o.id, o.order_no, o.detail_fetch_status, o.detail_fetch_error
ORDER BY COUNT(d.id) DESC;


-- -----------------------------------------------------------------------------
-- 10. 最近入库明细
-- -----------------------------------------------------------------------------
SELECT d.order_no             AS "订单编号",
       d.crm_detail_id        AS "CRM明细ID",
       d.detail_name          AS "明细编号",
       d.pd_code              AS "商品编码",
       d.pd_count             AS "商品数量",
       d.actual_price         AS "实际售价",
       d.oa_sync_status       AS "OA同步状态",
       d.created_at           AS "入库时间"
FROM t_integration_crm_order_detail d
WHERE d.is_deleted = 0
ORDER BY d.created_at DESC
LIMIT 100;


-- -----------------------------------------------------------------------------
-- 11. 订单 ↔ 明细数量统计（Top）
-- -----------------------------------------------------------------------------
SELECT o.order_no             AS "订单编号",
       o.crm_order_id         AS "CRM订单ID",
       o.detail_fetch_status  AS "明细拉取状态",
       COUNT(d.id)            AS "明细行数",
       o.created_at           AS "入库时间"
FROM t_integration_crm_order o
LEFT JOIN t_integration_crm_order_detail d
       ON d.order_id = o.id AND d.is_deleted = 0
WHERE o.is_deleted = 0
GROUP BY o.id, o.order_no, o.crm_order_id, o.detail_fetch_status, o.created_at
ORDER BY COUNT(d.id) DESC, o.created_at DESC
LIMIT 50;


-- -----------------------------------------------------------------------------
-- 12. 主表原始报文（JSONB）
-- -----------------------------------------------------------------------------
SELECT order_no                                    AS "订单编号",
       crm_order_id                                AS "CRM订单ID",
       raw_payload                                 AS "订单原始报文",
       raw_payload -> 'customer'                   AS "客户对象JSON",
       raw_payload -> 'owner'                      AS "负责人对象JSON",
       raw_payload ->> 'order_total_amount'        AS "原始订单总额",
       raw_payload -> 'modify_time'                AS "原始修改时间"
FROM t_integration_crm_order
WHERE is_deleted = 0
  AND order_no = '你的订单编号'   -- TODO 替换
;


-- -----------------------------------------------------------------------------
-- 13. 明细原始报文（JSONB）
-- -----------------------------------------------------------------------------
SELECT order_no                                    AS "订单编号",
       crm_detail_id                              AS "CRM明细ID",
       pd_code                                     AS "商品编码",
       raw_payload                                 AS "明细原始报文",
       raw_payload ->> 'pd_code'                   AS "原始商品编码",
       raw_payload ->> 'pd_count'                  AS "原始商品数量",
       raw_payload ->> 'actual_price'              AS "原始实际售价",
       raw_payload -> 'order_id'                   AS "原始关联订单对象"
FROM t_integration_crm_order_detail
WHERE is_deleted = 0
  AND order_no = '你的订单编号'   -- TODO 替换
ORDER BY id;


-- -----------------------------------------------------------------------------
-- 14. 去重键冲突排查（正常应无结果）
-- -----------------------------------------------------------------------------
SELECT order_no AS "订单编号",
       COUNT(*) AS "重复条数"
FROM t_integration_crm_order
WHERE is_deleted = 0
GROUP BY order_no
HAVING COUNT(*) > 1;

SELECT crm_detail_id AS "CRM明细ID",
       COUNT(*)      AS "重复条数"
FROM t_integration_crm_order_detail
WHERE is_deleted = 0
GROUP BY crm_detail_id
HAVING COUNT(*) > 1;


-- -----------------------------------------------------------------------------
-- 15. 按负责人核对（人员映射前置）
-- -----------------------------------------------------------------------------
SELECT owner_id      AS "负责人员工ID",
       owner_name    AS "负责人姓名",
       COUNT(*)      AS "订单数量",
       COUNT(*) FILTER (WHERE detail_fetch_status = 'SUCCESS') AS "明细已成功数",
       COUNT(*) FILTER (WHERE detail_fetch_status IN ('PENDING', 'FAILED')) AS "明细待补拉数"
FROM t_integration_crm_order
WHERE is_deleted = 0
GROUP BY owner_id, owner_name
ORDER BY COUNT(*) DESC
LIMIT 50;


-- =============================================================================
-- 字段中文对照（表结构速查）
-- =============================================================================
-- 【主表 t_integration_crm_order】
-- id                    主键ID（雪花）
-- crm_order_id          CRM订单内部ID（接口 id）
-- order_no              订单编号（接口 name，业务去重键）
-- customer_id           客户ID
-- customer_name         客户名称
-- owner_id              负责人员工ID（人员映射入口）
-- owner_name            负责人姓名
-- currency_type         币种
-- order_total_amount    订单总额
-- crm_create_time       CRM创建时间
-- crm_modify_time       CRM修改时间（采集过滤字段）
-- detail_fetch_status   明细拉取状态：PENDING/SUCCESS/FAILED
-- detail_fetch_error    明细拉取失败原因
-- detail_fetch_time     明细最近拉取时间
-- raw_payload           订单完整原始报文（JSONB）
-- created_at            入库时间
-- updated_at            更新时间
-- is_deleted            逻辑删除：0正常 1删除
--
-- 【明细表 t_integration_crm_order_detail】
-- id                    主键ID（雪花）
-- order_id              关联主表主键
-- crm_order_id          CRM订单内部ID（冗余）
-- order_no              订单编号（冗余）
-- crm_detail_id         CRM明细ID（去重键）
-- detail_name           明细编号
-- pd_code               商品编码
-- pd_count              商品数量
-- actual_price          实际售价
-- material_code         物料编号
-- oa_sync_status        OA同步状态：PENDING/SUCCESS/RETRY/FAILED
-- oa_process_id         OA流程实例ID
-- oa_sync_time          OA最近同步时间
-- retry_count           OA重试次数
-- last_error_msg        最近错误信息
-- raw_payload           明细完整原始报文（JSONB）
-- created_at            入库时间
-- updated_at            更新时间
-- is_deleted            逻辑删除：0正常 1删除
