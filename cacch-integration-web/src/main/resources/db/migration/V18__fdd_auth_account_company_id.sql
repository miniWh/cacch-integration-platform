-- 法大大认证记录补充 accountId / companyId，对齐「先创建用户/企业再实名」流程
ALTER TABLE t_integration_fdd_person_auth
    ADD COLUMN IF NOT EXISTS fdd_account_id VARCHAR(64);

COMMENT ON COLUMN t_integration_fdd_person_auth.fdd_account_id IS '法大大本地用户唯一标识 accountId';

ALTER TABLE t_integration_fdd_enterprise_auth
    ADD COLUMN IF NOT EXISTS fdd_company_id VARCHAR(64);

ALTER TABLE t_integration_fdd_enterprise_auth
    ADD COLUMN IF NOT EXISTS fdd_account_id VARCHAR(64);

COMMENT ON COLUMN t_integration_fdd_enterprise_auth.fdd_company_id IS '法大大本地企业唯一标识 companyId';
COMMENT ON COLUMN t_integration_fdd_enterprise_auth.fdd_account_id IS '企业管理员法大大本地用户唯一标识 accountId';
