-- =============================================
-- V19: 个人认证业务键升级为 internal_company_name + id_number + mobile
-- 同一内部企业下，换手机号视为未认证（可另起 SUCCESS）
-- =============================================

-- 历史 mobile 为空的 SUCCESS 无法纳入新唯一键，先占位（若有多条需人工处理）
UPDATE t_integration_fdd_person_auth
SET mobile = '00000000000'
WHERE mobile IS NULL OR btrim(mobile) = '';

ALTER TABLE t_integration_fdd_person_auth
    ALTER COLUMN mobile SET NOT NULL;

COMMENT ON COLUMN t_integration_fdd_person_auth.mobile IS '手机号（业务判定键之一，三要素明文）';
COMMENT ON COLUMN t_integration_fdd_person_auth.id_number IS '身份证号（业务判定键之一，明文）';
COMMENT ON COLUMN t_integration_fdd_person_auth.internal_company_name IS '内部企业全称（业务判定键之一）';

DROP INDEX IF EXISTS uk_fdd_person_auth_success;

CREATE UNIQUE INDEX IF NOT EXISTS uk_fdd_person_auth_success
    ON t_integration_fdd_person_auth (internal_company_name, id_number, mobile)
    WHERE is_deleted = 0 AND auth_status = 'SUCCESS';

DROP INDEX IF EXISTS idx_fdd_person_auth_biz_key;

CREATE INDEX IF NOT EXISTS idx_fdd_person_auth_biz_key
    ON t_integration_fdd_person_auth (internal_company_name, id_number, mobile, auth_status, created_at DESC)
    WHERE is_deleted = 0;
