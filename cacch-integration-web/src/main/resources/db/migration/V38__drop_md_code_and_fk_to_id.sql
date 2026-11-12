-- =============================================
-- V38: 删 md_code 统一用 id 外键
--   前置: V35/V36/V37 已在所有环境执行（不可修改）
--   数据库: PostgreSQL 12.22（PG12 不支持 ADD CONSTRAINT IF NOT EXISTS，用 DO 块替代）
--   变更:
--     - mdm_emp: DROP md_code + uk_mdm_emp_md_code
--     - mdm_emp_org_assign: md_code → emp_id（复制数据后删列）
--     - mdm_emp_event: md_code → emp_id
--     - mdm_emp_src_ref: md_code → emp_id
--   注: 空串→NULL 的历史数据清理由上线前运维脚本完成，本脚本仅做结构变更；
--       应用层（MdmEmpConverter nullIfBlank）已从源头杜绝空串入库
-- =============================================

-- ---------- 1. mdm_emp 删 md_code ----------
ALTER TABLE mdm_emp DROP CONSTRAINT IF EXISTS uk_mdm_emp_md_code;
ALTER TABLE mdm_emp DROP COLUMN IF EXISTS md_code;

-- ---------- 2. mdm_emp_org_assign: md_code → emp_id ----------
ALTER TABLE mdm_emp_org_assign ADD COLUMN IF NOT EXISTS emp_id BIGINT;
UPDATE mdm_emp_org_assign SET emp_id = md_code WHERE emp_id IS NULL;
ALTER TABLE mdm_emp_org_assign DROP COLUMN IF EXISTS md_code;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_mdm_emp_org_assign_emp'
    ) THEN
        ALTER TABLE mdm_emp_org_assign
            ADD CONSTRAINT fk_mdm_emp_org_assign_emp
            FOREIGN KEY (emp_id) REFERENCES mdm_emp(id);
    END IF;
END $$;
DROP INDEX IF EXISTS idx_mdm_emp_org_assign_md_code;
CREATE INDEX IF NOT EXISTS idx_mdm_emp_org_assign_emp_id
    ON mdm_emp_org_assign(emp_id);
DROP INDEX IF EXISTS idx_mdm_emp_org_assign_active;
CREATE INDEX IF NOT EXISTS idx_mdm_emp_org_assign_active
    ON mdm_emp_org_assign(emp_id) WHERE valid_to IS NULL AND is_deleted = 0;

COMMENT ON COLUMN mdm_emp_org_assign.emp_id IS '关联 mdm_emp.id';

-- ---------- 3. mdm_emp_event: md_code → emp_id ----------
ALTER TABLE mdm_emp_event ADD COLUMN IF NOT EXISTS emp_id BIGINT;
UPDATE mdm_emp_event SET emp_id = md_code WHERE emp_id IS NULL;
ALTER TABLE mdm_emp_event DROP COLUMN IF EXISTS md_code;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_mdm_emp_event_emp'
    ) THEN
        ALTER TABLE mdm_emp_event
            ADD CONSTRAINT fk_mdm_emp_event_emp
            FOREIGN KEY (emp_id) REFERENCES mdm_emp(id);
    END IF;
END $$;
DROP INDEX IF EXISTS idx_mdm_emp_event_md_code;
CREATE INDEX IF NOT EXISTS idx_mdm_emp_event_emp_id
    ON mdm_emp_event(emp_id);

COMMENT ON COLUMN mdm_emp_event.emp_id IS '关联 mdm_emp.id';

-- ---------- 4. mdm_emp_src_ref: md_code → emp_id ----------
ALTER TABLE mdm_emp_src_ref ADD COLUMN IF NOT EXISTS emp_id BIGINT;
UPDATE mdm_emp_src_ref SET emp_id = md_code WHERE emp_id IS NULL;
ALTER TABLE mdm_emp_src_ref DROP COLUMN IF EXISTS md_code;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_mdm_emp_src_ref_emp'
    ) THEN
        ALTER TABLE mdm_emp_src_ref
            ADD CONSTRAINT fk_mdm_emp_src_ref_emp
            FOREIGN KEY (emp_id) REFERENCES mdm_emp(id);
    END IF;
END $$;
DROP INDEX IF EXISTS idx_mdm_emp_src_ref_md_code;
CREATE INDEX IF NOT EXISTS idx_mdm_emp_src_ref_emp_id
    ON mdm_emp_src_ref(emp_id);

COMMENT ON COLUMN mdm_emp_src_ref.emp_id IS '关联 mdm_emp.id';
