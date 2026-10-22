-- =============================================
-- V30: 客如云订单同步任务控制表（按天粒度）
--   用途：手动触发同步时，先为指定日期创建 PENDING 任务，
--        再逐条拉取客如云订单数据落库；已存在日期不重复创建。
--   唯一键：(sync_date, date_type)
--   状态机：PENDING → IN_PROGRESS → SUCCESS / FAILED / PARTIAL
--   断点续传：last_page 记录已拉取到的最后页码
--   乐观锁：version 字段保证状态流转的并发安全
-- =============================================

CREATE TABLE IF NOT EXISTS stg_keruyun_order_sync_task (
    id                  BIGINT        NOT NULL,
    sync_date           DATE          NOT NULL,
    date_type           VARCHAR(32)   NOT NULL,
    sync_window_start   TIMESTAMP     NOT NULL,
    sync_window_end     TIMESTAMP     NOT NULL,
    status              VARCHAR(16)   NOT NULL,
    retry_count         INT           NOT NULL DEFAULT 0,
    last_page           INT           NOT NULL DEFAULT 0,
    total_page          INT,
    total_count         INT,
    success_count       INT,
    failure_count       INT,
    error_message       VARCHAR(500),
    version             INT           NOT NULL DEFAULT 0,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    synced_at           TIMESTAMP,
    is_deleted          SMALLINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_stg_keruyun_order_sync_task PRIMARY KEY (id),
    CONSTRAINT uk_stg_keruyun_order_sync_task_date_type UNIQUE (sync_date, date_type)
);

COMMENT ON TABLE  stg_keruyun_order_sync_task IS '客如云订单同步任务控制表';
COMMENT ON COLUMN stg_keruyun_order_sync_task.id                  IS '内部主键';
COMMENT ON COLUMN stg_keruyun_order_sync_task.sync_date           IS '同步日期';
COMMENT ON COLUMN stg_keruyun_order_sync_task.date_type           IS '查询时间类型：FINISH_BUSI_DATE / OPEN_TIME / FINISH_TIME / SETTLE_TIME';
COMMENT ON COLUMN stg_keruyun_order_sync_task.sync_window_start   IS '实际查询窗口起始（sync_date 00:00:00）';
COMMENT ON COLUMN stg_keruyun_order_sync_task.sync_window_end     IS '实际查询窗口结束（sync_date 23:59:59）';
COMMENT ON COLUMN stg_keruyun_order_sync_task.status              IS '任务状态：PENDING-待执行 / IN_PROGRESS-执行中 / SUCCESS-成功 / FAILED-失败 / PARTIAL-部分失败';
COMMENT ON COLUMN stg_keruyun_order_sync_task.retry_count         IS '已重试次数';
COMMENT ON COLUMN stg_keruyun_order_sync_task.last_page           IS '断点续传用，已拉取到的最后页码';
COMMENT ON COLUMN stg_keruyun_order_sync_task.total_page          IS '总页数（首次执行后回填）';
COMMENT ON COLUMN stg_keruyun_order_sync_task.total_count         IS '总订单数（首次执行后回填）';
COMMENT ON COLUMN stg_keruyun_order_sync_task.success_count       IS '落库成功数';
COMMENT ON COLUMN stg_keruyun_order_sync_task.failure_count       IS '落库失败数';
COMMENT ON COLUMN stg_keruyun_order_sync_task.error_message       IS '失败原因';
COMMENT ON COLUMN stg_keruyun_order_sync_task.version             IS '乐观锁版本号';
COMMENT ON COLUMN stg_keruyun_order_sync_task.created_at         IS '任务创建时间';
COMMENT ON COLUMN stg_keruyun_order_sync_task.updated_at         IS '任务最近状态/进度更新时间';
COMMENT ON COLUMN stg_keruyun_order_sync_task.synced_at           IS '任务执行完成时间';
COMMENT ON COLUMN stg_keruyun_order_sync_task.is_deleted          IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_stg_keruyun_order_sync_task_status
    ON stg_keruyun_order_sync_task(status) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_stg_keruyun_order_sync_task_date
    ON stg_keruyun_order_sync_task(sync_date) WHERE is_deleted = 0;
