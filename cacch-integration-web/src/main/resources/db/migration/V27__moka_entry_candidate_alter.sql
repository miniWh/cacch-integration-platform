-- =============================================
-- V27: Moka 待入职快照表字段补全与清理
--   - 主表新增 archived（申请是否归档）
--   - 主表删除 moka_job_department（list_by_condition 的 job 不含 department，恒为 null）
--   - 毕业院校子表新增 school_city_id
--   - 本脚本仅 ALTER，V26 可能已在 test 执行，禁止修改 V26
-- =============================================


-- ---------- 主表：删 moka_job_department + 加 archived ----------

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 't_integration_moka_entry_application'
          AND column_name = 'moka_job_department'
    ) THEN
        ALTER TABLE t_integration_moka_entry_application DROP COLUMN moka_job_department;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 't_integration_moka_entry_application'
          AND column_name = 'archived'
    ) THEN
        ALTER TABLE t_integration_moka_entry_application ADD COLUMN archived SMALLINT DEFAULT 0;
        COMMENT ON COLUMN t_integration_moka_entry_application.archived IS '申请是否归档：0=未归档 1=已归档（list_by_condition 顶层 archived 字段）';
    END IF;
END $$;


-- ---------- 毕业院校子表：加 school_city_id ----------

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 't_integration_moka_entry_graduate_school'
          AND column_name = 'school_city_id'
    ) THEN
        ALTER TABLE t_integration_moka_entry_graduate_school ADD COLUMN school_city_id INTEGER;
        COMMENT ON COLUMN t_integration_moka_entry_graduate_school.school_city_id IS '学校所在城市 ID（Moka graduateSchool.schoolCityId）';
    END IF;
END $$;
