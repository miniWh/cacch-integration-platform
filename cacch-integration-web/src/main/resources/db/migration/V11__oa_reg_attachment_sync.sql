-- =============================================
-- V11: 国内登记报告资料列表附件同步中间表（REQ-OA-001）
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_oa_reg_attachment_sync (
    id                  BIGINT        NOT NULL,
    form_main_id        BIGINT,
    owner_name          VARCHAR(128)  NOT NULL,
    ipdp_name           VARCHAR(512)  NOT NULL,
    item_name           VARCHAR(512)  NOT NULL,
    item_row_id         BIGINT        NOT NULL,
    share_path          VARCHAR(1024),
    file_name           VARCHAR(512),
    file_version        INTEGER,
    file_size           BIGINT,
    file_checksum       VARCHAR(128),
    file_modified_at    TIMESTAMP,
    oa_file_id          VARCHAR(256),
    oa_sub_reference    VARCHAR(128),
    sync_status         VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    sync_message        TEXT,
    retry_count         INT           NOT NULL DEFAULT 0,
    last_sync_at        TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_oa_reg_attachment_sync PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_oa_reg_attachment_sync IS '国内登记报告资料列表附件同步记录——幂等键(owner_name,ipdp_name,item_name,file_version)';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.form_main_id     IS 'OA 主表 formmain_4070.id';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.owner_name       IS '登记负责人 field0223';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.ipdp_name        IS 'IPDP 名称 field0160';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.item_name        IS '资料项目 field0214';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.item_row_id        IS '子表行 formson_5464.id';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.share_path       IS '共享盘目录完整路径';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.file_name        IS '同步文件名';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.file_version     IS '解析版本号';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.file_checksum    IS '文件 SHA-256';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.oa_file_id       IS 'OA REST 上传返回 fileUrl';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.oa_sub_reference IS 'CAP4 绑定 subReference（field0218）';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.sync_status        IS 'PENDING/SUCCESS/RETRY/FAILED/SKIPPED';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.sync_message       IS '同步说明或失败原因';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.retry_count        IS '重试次数';
COMMENT ON COLUMN t_integration_oa_reg_attachment_sync.last_sync_at       IS '最近一次同步时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_oa_reg_att_sync_biz_key
    ON t_integration_oa_reg_attachment_sync (owner_name, ipdp_name, item_name, file_version)
    WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_oa_reg_att_sync_form_main
    ON t_integration_oa_reg_attachment_sync (form_main_id) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_oa_reg_att_sync_status
    ON t_integration_oa_reg_attachment_sync (sync_status, retry_count) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_oa_reg_att_sync_item_row
    ON t_integration_oa_reg_attachment_sync (item_row_id) WHERE is_deleted = 0;
