-- =============================================
-- V49: Heart 客户 PO 主表新增目的地列（REQ-HEART-001）
--   1. destination_name：目的地名称/查询关键字（来源邮件解析，为空时使用固定值）
--   2. destination_id：目的地 CRM 对象 ID（object_65Zui__c），
--      建单时写入 contractAdd 的 field_RHVAG__c
-- =============================================

ALTER TABLE t_integration_crm_heart_po
    ADD COLUMN IF NOT EXISTS destination_name VARCHAR(64),
    ADD COLUMN IF NOT EXISTS destination_id   VARCHAR(64);

COMMENT ON COLUMN t_integration_crm_heart_po.destination_name IS '目的地';
COMMENT ON COLUMN t_integration_crm_heart_po.destination_id   IS '目的地CRM对象ID';
