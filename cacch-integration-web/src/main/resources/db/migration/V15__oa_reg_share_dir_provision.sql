-- =============================================
-- V15: 共享盘目录治理记录表（REQ-OA-002）
-- append 模式：每轮执行新增记录，保留全部历史
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_oa_reg_share_dir_provision (
    id                  BIGINT        NOT NULL,
    run_id              VARCHAR(64)   NOT NULL,
    form_main_id        BIGINT,
    owner_name          VARCHAR(128),
    ipdp_name           VARCHAR(512),
    ipdp_project_no     VARCHAR(128),
    item_name           VARCHAR(512),
    item_row_id         VARCHAR(64),
    item_required       VARCHAR(16),
    share_path          VARCHAR(1024),
    group_retain        BOOLEAN,
    action              VARCHAR(32)   NOT NULL,
    action_message      TEXT,
    provisioned_at      TIMESTAMP     NOT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_oa_reg_share_dir_provision PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_oa_reg_share_dir_provision IS '共享盘目录治理记录（append 模式）';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.run_id          IS '执行轮次标识（时间戳+UUID），同轮所有记录共享';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.form_main_id    IS 'OA 主表 formmain_4070.id';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.owner_name      IS '登记负责人 field0223';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.ipdp_name       IS 'IPDP 名称 field0160';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.ipdp_project_no IS 'IPDP 项目编号 field0164';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.item_name       IS '资料项目名称 field0214';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.item_row_id     IS '子表行 formson_5464.id';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.item_required   IS '需要/不需要快照（field0216 原始值：0=需要，1=不需要）';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.share_path      IS 'L3 完整路径（归一化后）';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.group_retain    IS '所属路径组 groupRetain 决策（true=保留/创建）';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.action          IS 'CREATED/DELETED/SKIPPED_EXISTS/SKIPPED_NOT_EMPTY/SKIPPED_NOT_REQUIRED/SKIPPED_GROUP_RETAINED/FAILED';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.action_message  IS '跳过或失败原因';
COMMENT ON COLUMN t_integration_oa_reg_share_dir_provision.provisioned_at  IS '本次治理时间';

CREATE INDEX IF NOT EXISTS idx_provision_biz_key
    ON t_integration_oa_reg_share_dir_provision (owner_name, ipdp_name, ipdp_project_no, item_name, provisioned_at DESC)
    WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_provision_form_action
    ON t_integration_oa_reg_share_dir_provision (form_main_id, action);

CREATE INDEX IF NOT EXISTS idx_provision_run
    ON t_integration_oa_reg_share_dir_provision (run_id);
