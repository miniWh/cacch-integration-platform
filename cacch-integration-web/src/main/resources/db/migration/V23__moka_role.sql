-- =============================================
-- V23: Moka 自定义角色中间表
--   对接 Moka 开放平台角色查询接口
--   GET /api-platform/v1/users/roles?type=all
--   接口 0 定时拉取全量角色落库，作为人员同步 roleId 匹配依据
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_moka_role (
    -- 内部主键（MyBatis-Plus 雪花生成；role_id 为业务主键，单独建 UNIQUE 约束）
    id                    BIGINT       NOT NULL,
    -- ========== Moka 业务字段（对应 API 响应 id / name / role / description） ==========
    role_id               INTEGER      NOT NULL,   -- Moka 角色 ID（业务唯一键，UNIQUE；对应 API id）
    role_name             VARCHAR(128),             -- Moka 角色名称（对应 API name）
    role                  INTEGER,                  -- Moka 角色值（整数；对应 API role）
    description           VARCHAR(500),             -- Moka 角色描述（对应 API description）
    -- ========== 审计字段 ==========
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted            SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_role PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_role_role_id UNIQUE (role_id)
);

COMMENT ON TABLE  t_integration_moka_role IS 'Moka自定义角色中间表（由 Moka 开放平台角色查询接口全量同步）';

COMMENT ON COLUMN t_integration_moka_role.id               IS '内部主键（雪花生成）';
COMMENT ON COLUMN t_integration_moka_role.role_id          IS 'Moka 角色 ID（业务唯一键，UNIQUE；对应 Moka API id）';
COMMENT ON COLUMN t_integration_moka_role.role_name         IS 'Moka 角色名称（对应 Moka API name）';
COMMENT ON COLUMN t_integration_moka_role.role             IS 'Moka 角色值（整数；对应 Moka API role）';
COMMENT ON COLUMN t_integration_moka_role.description      IS 'Moka 角色描述（对应 Moka API description）';
COMMENT ON COLUMN t_integration_moka_role.created_at       IS '记录创建时间';
COMMENT ON COLUMN t_integration_moka_role.updated_at       IS '记录更新时间';
COMMENT ON COLUMN t_integration_moka_role.is_deleted       IS '逻辑删除：0-正常 1-删除';

-- 按角色名称查询（人员同步阶段二按 jobTitle 匹配 roleId 时使用）
CREATE INDEX IF NOT EXISTS idx_t_integration_moka_role_name
    ON t_integration_moka_role(role_name) WHERE is_deleted = 0;
