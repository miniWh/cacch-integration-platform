-- =============================================
-- V48: Heart 客户 PO 主表港口列调整（REQ-HEART-001 F10 港口解析）
--   1. 新增启运港 / 目的港 CRM 对象 ID 列（object_yEQGF__c 记录 id）
--   2. 删除原单一 port_object_id（无法同时承载启运港与目的港）
-- =============================================

ALTER TABLE t_integration_crm_heart_po
    ADD COLUMN IF NOT EXISTS port_of_loading_id   VARCHAR(64),
    ADD COLUMN IF NOT EXISTS port_of_discharge_id VARCHAR(64);

COMMENT ON COLUMN t_integration_crm_heart_po.port_of_loading_id   IS '启运港CRM对象ID（object_yEQGF__c）';
COMMENT ON COLUMN t_integration_crm_heart_po.port_of_discharge_id IS '目的港CRM对象ID（object_yEQGF__c）';

ALTER TABLE t_integration_crm_heart_po
    DROP COLUMN IF EXISTS port_object_id;
