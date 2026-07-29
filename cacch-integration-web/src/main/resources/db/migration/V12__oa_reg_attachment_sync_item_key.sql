-- =============================================
-- V12: 附件同步幂等改为资料项维度，按共享盘文件创建时间比对
-- =============================================

ALTER TABLE t_integration_oa_reg_attachment_sync
    ADD COLUMN IF NOT EXISTS file_created_at TIMESTAMP;

COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.file_created_at IS '共享盘文件创建时间（幂等比对）';

UPDATE t_integration_oa_reg_attachment_sync
SET file_created_at = COALESCE(file_created_at, file_modified_at)
WHERE is_deleted = 0;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY owner_name, ipdp_name, item_name
               ORDER BY last_sync_at DESC NULLS LAST, id DESC
           ) AS rn
    FROM t_integration_oa_reg_attachment_sync
    WHERE is_deleted = 0
)
UPDATE t_integration_oa_reg_attachment_sync t
SET is_deleted = 1,
    updated_at = CURRENT_TIMESTAMP
FROM ranked r
WHERE t.id = r.id
  AND r.rn > 1;

DROP INDEX IF EXISTS uk_oa_reg_att_sync_biz_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_oa_reg_att_sync_item_key
    ON t_integration_oa_reg_attachment_sync (owner_name, ipdp_name, item_name)
    WHERE is_deleted = 0;

COMMENT ON TABLE t_integration_oa_reg_attachment_sync IS '国内登记报告资料列表附件同步记录——幂等键(owner_name,ipdp_name,item_name)+file_created_at';
