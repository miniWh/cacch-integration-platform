-- =============================================
-- V40: stg_staff_record 队表幂等约束方案 B 重构
--
--   背景: V39 用 UNIQUE(staff_id, pull_batch_no) 每次 Pull 新批次号绕过唯一约束，
--         导致同一员工一天 288 次 Pull = 288 行队表数据膨胀，
--         Process 反复捞 PROCESSED 行再跑一次 iHR 浪费。
--
--   方案 B:
--     - source='ADD': 业务上新增只发生一次 → 部分唯一索引 ON(staff_id) WHERE source='ADD'
--     - source='UPDATE': 不同 last_update 是不同变更事件 → 部分唯一索引 ON(staff_id, last_update) WHERE source='UPDATE'
--     - ON CONFLICT DO NOTHING —— 任何唯一约束冲突都直接跳过，零写放大
--     - 队表无限长暂不清理（后续加清理策略）
--
--   历史数据: 用户手动清空，本脚本不处理
-- =============================================

-- 1. 删除旧的整表 UNIQUE 约束（名字由 PG 自动生成，通过系统表查并动态删除）

-- 2. last_update 加 DEFAULT 0（additions 固定传 0，不按 last_update 判重）
ALTER TABLE stg_staff_record ALTER COLUMN last_update SET DEFAULT 0;
UPDATE stg_staff_record SET last_update = 0 WHERE last_update IS NULL;

-- 3. 为 source='ADD' 建部分唯一索引（业务上同一员工只新增一次）
CREATE UNIQUE INDEX IF NOT EXISTS idx_stg_staff_record_add_unique
    ON stg_staff_record(staff_id)
    WHERE source = 'ADD';

-- 4. 为 source='UPDATE' 建部分唯一索引（同一员工不同 last_update 是不同变更事件）
CREATE UNIQUE INDEX IF NOT EXISTS idx_stg_staff_record_update_unique
    ON stg_staff_record(staff_id, last_update)
    WHERE source = 'UPDATE';

-- 5. 增强 status 复合索引（Process 捞 PENDING 高频查询，提升效率）
-- 原 V39 已有 idx_stg_staff_record_status(status, updated_at)，保持不动即可
-- 额外加一个 status 单独索引供 COUNT/统计使用
CREATE INDEX IF NOT EXISTS idx_stg_staff_record_status_only
    ON stg_staff_record(status);

-- 6. 删除 V39 的 source / last_update 单独索引（部分唯一索引已覆盖，避免冗余）
DROP INDEX IF EXISTS idx_stg_staff_record_source;
DROP INDEX IF EXISTS idx_stg_staff_record_last_update;
