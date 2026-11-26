-- =============================================
-- V50: Heart 客户 PO 明细表新增 BOM 版本列（REQ-HEART-001）
--   bom_version_id：BOM 版本 ID（object_XbTYD__c），
--   建单时写入 contractAdd 明细的 field_6mBjw__c
-- =============================================

ALTER TABLE t_integration_crm_heart_po_item
    ADD COLUMN IF NOT EXISTS bom_version_id VARCHAR(64);

COMMENT ON COLUMN t_integration_crm_heart_po_item.bom_version_id IS 'BOM版本ID';
