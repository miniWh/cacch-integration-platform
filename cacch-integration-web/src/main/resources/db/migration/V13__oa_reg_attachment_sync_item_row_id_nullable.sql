-- =============================================
-- V13: item_row_id 允许为空（共享盘已扫描但 OA 未匹配时无子表行 ID）
-- =============================================

ALTER TABLE t_integration_oa_reg_attachment_sync
    ALTER COLUMN item_row_id DROP NOT NULL;

COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.item_row_id IS '子表行 formson_5464.id；OA 未匹配时可空';
