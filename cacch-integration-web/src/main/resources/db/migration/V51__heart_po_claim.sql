-- =============================================
-- V51: Heart 客户 PO 主表新增流程认领列（REQ-HEART-001）
--   claim_token：流程认领令牌，防并发重复处理
--   claim_at   ：认领时间，超过 10 分钟视为失效，可被重新认领
-- =============================================

ALTER TABLE t_integration_crm_heart_po
    ADD COLUMN IF NOT EXISTS claim_token VARCHAR(64),
    ADD COLUMN IF NOT EXISTS claim_at  TIMESTAMP;

COMMENT ON COLUMN t_integration_crm_heart_po.claim_token IS '流程认领令牌';
COMMENT ON COLUMN t_integration_crm_heart_po.claim_at IS '流程认领时间';
