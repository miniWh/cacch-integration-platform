-- =============================================
-- V14: 幂等键增加 IPDP 项目编号 field0164，避免同负责人多 IPDP 误绑
-- =============================================

ALTER TABLE t_integration_oa_reg_attachment_sync
    ADD COLUMN IF NOT EXISTS ipdp_project_no VARCHAR(128);

COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.ipdp_project_no IS 'IPDP 项目编号 field0164';

UPDATE t_integration_oa_reg_attachment_sync
SET ipdp_project_no = COALESCE(NULLIF(TRIM(ipdp_project_no), ''), 'UNKNOWN')
WHERE is_deleted = 0
  AND (ipdp_project_no IS NULL OR TRIM(ipdp_project_no) = '');

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY owner_name, ipdp_name, ipdp_project_no, item_name
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

DROP INDEX IF EXISTS uk_oa_reg_att_sync_item_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_oa_reg_att_sync_item_key
    ON t_integration_oa_reg_attachment_sync (owner_name, ipdp_name, ipdp_project_no, item_name)
    WHERE is_deleted = 0;

COMMENT ON TABLE t_integration_oa_reg_attachment_sync IS '国内登记报告资料列表附件同步记录——幂等键(owner_name,ipdp_name,ipdp_project_no,item_name)+file_created_at';
