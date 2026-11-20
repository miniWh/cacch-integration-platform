-- V45: Moka 人员中间表 —— 对账快照字段 + 同步状态 4（跳过自动推送）

ALTER TABLE t_integration_moka_person
    ADD COLUMN IF NOT EXISTS moka_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS moka_builtin_role SMALLINT,
    ADD COLUMN IF NOT EXISTS moka_role_synced_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS moka_exists SMALLINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN t_integration_moka_person.moka_user_id IS 'Moka 平台用户 ID（获取人事信息 V3 userList.userId）';
COMMENT ON COLUMN t_integration_moka_person.moka_builtin_role IS 'Moka 内建角色值 role（0/5/10 等，用于自动推送门禁）';
COMMENT ON COLUMN t_integration_moka_person.moka_role_synced_at IS '最近一次从 Moka V3 拉取人事信息的时间';
COMMENT ON COLUMN t_integration_moka_person.moka_exists IS 'Moka 存在性：0-未知 1-平台存在 2-对账未命中（预留）';

COMMENT ON COLUMN t_integration_moka_person.moka_sync_status IS
    'Moka 开放平台同步状态：0-PENDING 1-SYNCED 2-SYNC_FAILED 4-SKIP（非0/5/10角色由业务在Moka维护）';

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_contact_phone
    ON t_integration_moka_person(contact_phone) WHERE is_deleted = 0;