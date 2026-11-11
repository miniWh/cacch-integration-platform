-- =============================================
-- V34: Moka 部门表新增同步/删除时间字段
--   1. 新增 moka_sync_time 列（最近一次推送 Moka 成功时间）
--   2. 新增 moka_delete_time 列（Moka 侧删除成功时间）
--   3. moka_sync_status 状态注释扩展：新增 3-待删除 4-已删除
-- =============================================

-- 1) 新增推送成功时间列
ALTER TABLE t_integration_moka_department
    ADD COLUMN IF NOT EXISTS moka_sync_time TIMESTAMP;

-- 2) 新增 Moka 侧删除成功时间列
--    iHR 重新启用已删部门时，upsert 会将该列置回 NULL（部门复活）
ALTER TABLE t_integration_moka_department
    ADD COLUMN IF NOT EXISTS moka_delete_time TIMESTAMP;

COMMENT ON COLUMN t_integration_moka_department.moka_sync_time
    IS '最近一次推送 Moka 成功时间';

COMMENT ON COLUMN t_integration_moka_department.moka_delete_time
    IS 'Moka 侧删除成功时间（iHR 重新启用部门复活时置 NULL）';

-- 3) 同步状态注释扩展：0-未同步 1-已同步 2-同步失败 3-待删除 4-已删除
COMMENT ON COLUMN t_integration_moka_department.moka_sync_status
    IS 'Moka开放平台同步状态：0-未同步 1-已同步 2-同步失败 3-待删除 4-已删除';
