-- =============================================
-- V1: 初始化基础表结构
-- =============================================

-- API Key 认证表
CREATE TABLE IF NOT EXISTS t_integration_api_key (
    id              BIGINT       NOT NULL,
    client_name     VARCHAR(100) NOT NULL,
    api_key         VARCHAR(255) NOT NULL,
    api_key_hash    VARCHAR(255) NOT NULL,
    status          SMALLINT     NOT NULL DEFAULT 1,
    description     VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_api_key PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_api_key IS 'API Key认证表';
COMMENT ON COLUMN t_integration_api_key.id            IS '主键';
COMMENT ON COLUMN t_integration_api_key.client_name   IS '调用方名称';
COMMENT ON COLUMN t_integration_api_key.api_key       IS 'API Key前缀（短标识）';
COMMENT ON COLUMN t_integration_api_key.api_key_hash  IS 'API Key BCrypt哈希';
COMMENT ON COLUMN t_integration_api_key.status        IS '状态：1-启用 0-禁用';
COMMENT ON COLUMN t_integration_api_key.description   IS '描述';
COMMENT ON COLUMN t_integration_api_key.created_at    IS '创建时间';
COMMENT ON COLUMN t_integration_api_key.updated_at    IS '更新时间';
COMMENT ON COLUMN t_integration_api_key.is_deleted    IS '逻辑删除：0-正常 1-删除';

-- API Key 索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_api_key_hash   ON t_integration_api_key(api_key_hash) WHERE is_deleted = 0;
CREATE INDEX        IF NOT EXISTS idx_api_key_status ON t_integration_api_key(status)        WHERE is_deleted = 0;

-- =============================================
-- 系统配置表
-- =============================================
CREATE TABLE IF NOT EXISTS t_integration_sys_config (
    id              BIGINT       NOT NULL,
    config_key      VARCHAR(100) NOT NULL,
    config_value    TEXT         NOT NULL,
    description     VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_sys_config PRIMARY KEY (id),
    CONSTRAINT uk_sys_config_key UNIQUE (config_key)
);

COMMENT ON TABLE  t_integration_sys_config IS '系统配置表';
COMMENT ON COLUMN t_integration_sys_config.config_key   IS '配置键';
COMMENT ON COLUMN t_integration_sys_config.config_value IS '配置值';

-- =============================================
-- 集成日志表
-- =============================================
CREATE TABLE IF NOT EXISTS t_integration_log (
    id              BIGINT       NOT NULL,
    trace_id        VARCHAR(64),
    source_system   VARCHAR(100) NOT NULL,
    target_system   VARCHAR(100) NOT NULL,
    interface_name  VARCHAR(200) NOT NULL,
    request_data    TEXT,
    response_data   TEXT,
    status          SMALLINT     NOT NULL DEFAULT 0,
    error_message   TEXT,
    cost_ms         BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_log PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_log IS '集成日志表';
COMMENT ON COLUMN t_integration_log.trace_id       IS '链路追踪ID';
COMMENT ON COLUMN t_integration_log.source_system  IS '来源系统';
COMMENT ON COLUMN t_integration_log.target_system  IS '目标系统';
COMMENT ON COLUMN t_integration_log.interface_name IS '调用接口名';
COMMENT ON COLUMN t_integration_log.status         IS '状态：0-成功 1-失败';
COMMENT ON COLUMN t_integration_log.cost_ms        IS '耗时(毫秒)';

-- 集成日志索引
CREATE INDEX IF NOT EXISTS idx_integration_log_trace   ON t_integration_log(trace_id)       WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_integration_log_created ON t_integration_log(created_at)     WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_integration_log_status  ON t_integration_log(status, created_at) WHERE is_deleted = 0;
