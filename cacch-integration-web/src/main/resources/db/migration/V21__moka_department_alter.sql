-- =============================================
-- V21: Moka 部门表结构调整
--   1. sequence 类型 NUMERIC(10,2) → INTEGER
--   2. 新增 moka_sync_status 列（Moka 开放平台同步状态）
-- =============================================

-- 1) sequence 列：NUMERIC(10,2) → INTEGER
--    先允许 NULL，再 CAST，再恢复 NOT NULL + DEFAULT
DO $$
BEGIN
    -- 检查列类型是否已经是 INTEGER（避免重复执行报错）
    IF (SELECT data_type FROM information_schema.columns
        WHERE table_name = 't_integration_moka_department'
          AND column_name = 'sequence') = 'numeric' THEN
        -- 允许临时为 NULL 以处理转换失败行
        ALTER TABLE t_integration_moka_department ALTER COLUMN sequence DROP NOT NULL;
        -- 转换：取整（舍去小数部分）
        ALTER TABLE t_integration_moka_department ALTER COLUMN sequence TYPE INTEGER
            USING CASE WHEN sequence IS NULL THEN NULL ELSE TRUNC(sequence)::INTEGER END;
        -- 恢复默认值 + NOT NULL
        ALTER TABLE t_integration_moka_department ALTER COLUMN sequence SET DEFAULT 9999;
        ALTER TABLE t_integration_moka_department ALTER COLUMN sequence SET NOT NULL;
    END IF;
END $$;

-- 2) 新增 Moka 开放平台同步状态列
--    0=PENDING(未同步) / 1=SYNCED(已同步) / 2=SYNC_FAILED(同步失败)
ALTER TABLE t_integration_moka_department
    ADD COLUMN IF NOT EXISTS moka_sync_status SMALLINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN t_integration_moka_department.moka_sync_status
    IS 'Moka开放平台同步状态：0-未同步 1-已同步 2-同步失败';

-- 同步状态索引（查询未同步/失败记录加速）
CREATE INDEX IF NOT EXISTS idx_t_integration_moka_dept_sync_status
    ON t_integration_moka_department(moka_sync_status);
