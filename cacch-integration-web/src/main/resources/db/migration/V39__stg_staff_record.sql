-- =============================================
-- V39: 新建 stg_staff_record 表（iHR 增量员工暂存队列表）
--   用途: Pull 任务把新增/更新员工 ID 入队 → Process 任务捞取 → 调 ingestOne 落库
--   特性:
--     - 入队幂等 UNIQUE(staff_id, pull_batch_no) + DO UPDATE 保留 ADD 优先级 + last_update 取较大值
--     - Process 捞取用 FOR UPDATE SKIP LOCKED 防并发
--     - 新增数据 source='ADD' 必须先于更新 source='UPDATE' 处理
--     - UPDATE 内部按 last_update 秒级时间戳升序（早变更先落库）
-- =============================================

CREATE TABLE IF NOT EXISTS stg_staff_record (
    id              BIGINT PRIMARY KEY,
    staff_id        VARCHAR(36)  NOT NULL,
    source          VARCHAR(10)  NOT NULL,
    last_update     BIGINT,
    pull_batch_no   VARCHAR(64)  NOT NULL,
    process_batch_no VARCHAR(64),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT stg_staff_record_source_check
        CHECK (source IN ('ADD', 'UPDATE')),
    CONSTRAINT stg_staff_record_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED')),
    UNIQUE (staff_id, pull_batch_no)
);

COMMENT ON TABLE stg_staff_record IS 'iHR 增量员工暂存队列';
COMMENT ON COLUMN stg_staff_record.id IS '内部主键';
COMMENT ON COLUMN stg_staff_record.staff_id IS 'iHR 员工 UUID';
COMMENT ON COLUMN stg_staff_record.source IS '来源: ADD-新增接口 / UPDATE-更新接口';
COMMENT ON COLUMN stg_staff_record.last_update IS '更新接口 V2 返回的秒级时间戳（新增接口固定 0）';
COMMENT ON COLUMN stg_staff_record.pull_batch_no IS '拉取批次号（同一轮 pull 共享）';
COMMENT ON COLUMN stg_staff_record.process_batch_no IS '处理批次号';
COMMENT ON COLUMN stg_staff_record.status IS '处理状态: PENDING-待处理 / PROCESSING-处理中 / PROCESSED-已处理';

CREATE INDEX IF NOT EXISTS idx_stg_staff_record_status
    ON stg_staff_record(status, updated_at);
CREATE INDEX IF NOT EXISTS idx_stg_staff_record_order
    ON stg_staff_record(
        CASE source WHEN 'ADD' THEN 1 ELSE 2 END,
        COALESCE(last_update, 0) ASC
    );
