-- REQ-CRM-001 阶段二：OA 表单同步核对 SQL（中文别名）
-- 用途：核对 PENDING/RETRY 待同步明细、成功回写、失败重试与告警门禁

-- 1) 待同步队列（与定时任务扫描条件一致：PENDING/RETRY 且 retry_count < 3）
SELECT
    d.id                    AS 明细主键,
    d.order_no              AS 订单编号,
    d.crm_detail_id         AS CRM明细ID,
    d.oa_sync_status        AS OA同步状态,
    d.retry_count           AS 重试次数,
    d.last_error_msg        AS 最近错误,
    o.detail_fetch_status   AS 明细拉取状态,
    o.owner_id              AS 负责人员工ID,
    o.raw_payload -> 'creator_id' ->> 'id' AS 创建人员工ID,
    d.created_at            AS 创建时间
FROM t_integration_crm_order_detail d
JOIN t_integration_crm_order o ON o.id = d.order_id AND o.is_deleted = 0
WHERE d.is_deleted = 0
  AND d.oa_sync_status IN ('PENDING', 'RETRY')
  AND d.retry_count < 3
  AND o.detail_fetch_status = 'SUCCESS'
ORDER BY d.id
LIMIT 100;

-- 2) 本轮已成功同步（有流程实例 ID）
SELECT
    d.id             AS 明细主键,
    d.order_no       AS 订单编号,
    d.crm_detail_id  AS CRM明细ID,
    d.oa_process_id  AS OA流程实例ID,
    d.oa_sync_time   AS 同步时间
FROM t_integration_crm_order_detail d
WHERE d.is_deleted = 0
  AND d.oa_sync_status = 'SUCCESS'
ORDER BY d.oa_sync_time DESC NULLS LAST
LIMIT 50;

-- 3) 已失败需人工介入
SELECT
    d.id             AS 明细主键,
    d.order_no       AS 订单编号,
    d.crm_detail_id  AS CRM明细ID,
    d.retry_count    AS 重试次数,
    d.last_error_msg AS 最近错误,
    d.oa_sync_time   AS 最近同步时间
FROM t_integration_crm_order_detail d
WHERE d.is_deleted = 0
  AND d.oa_sync_status = 'FAILED'
ORDER BY d.oa_sync_time DESC NULLS LAST
LIMIT 50;

-- 4) 状态分布
SELECT
    oa_sync_status AS OA同步状态,
    COUNT(*)       AS 数量
FROM t_integration_crm_order_detail
WHERE is_deleted = 0
GROUP BY oa_sync_status
ORDER BY oa_sync_status;
