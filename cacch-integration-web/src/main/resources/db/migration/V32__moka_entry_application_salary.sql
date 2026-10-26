-- =============================================
-- V32: Moka 待入职主表新增 Offer 薪资字段
--   - t_integration_moka_entry_application 新增 salary_number
--   - Step 1c（Offer 扩展接口）回写 salaryNumber 原始字符串
--   - 幂等写法（与 V27 风格一致），禁止修改 V26
-- =============================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 't_integration_moka_entry_application'
          AND column_name = 'salary_number'
    ) THEN
        ALTER TABLE t_integration_moka_entry_application
            ADD COLUMN salary_number VARCHAR(64);
        COMMENT ON COLUMN t_integration_moka_entry_application.salary_number
            IS '【Step 1c 补全】Offer 薪资（Moka salaryNumber 原始字符串，如 15000）';
    END IF;
END $$;
