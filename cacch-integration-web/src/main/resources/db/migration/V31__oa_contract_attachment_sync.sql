-- =============================================
-- V31: OA 货物购销合同附件回传 CRM 中间记录表（REQ-OA-003）
--   用途：存储 OA formmain_5296 查询出的购销合同数据快照，
--        并维护回传 CRM 合同的同步状态，支撑同步状态查询与重试。
--   数据来源：formmain_5296（购销合同） + formmain_2817（CRM 来源销售订单）
--             + ctp_attachment（合同附件）
--   幂等键：oa_form_main_id（一个合同只有一个附件）
--   状态机：PENDING → PROCESSING → SUCCESS / RETRY / SKIPPED（无 FAILED 终态）
--   乐观锁：version 字段，配合状态条件抢占记录，防止并发重复处理
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_oa_contract_attachment_sync (
    id                   BIGINT        NOT NULL,
    oa_form_main_id      BIGINT        NOT NULL,
    oa_start_date        TIMESTAMP,
    flow_status          VARCHAR(16),
    nc_order_no          VARCHAR(64),
    oa_contract_no       VARCHAR(64),
    oa_attachment_ref    VARCHAR(64),
    oa_attachment_id     VARCHAR(64),
    attachment_file_id   VARCHAR(255),
    attachment_file_name VARCHAR(255),
    attachment_url       VARCHAR(1000),
    crm_contract_id      VARCHAR(64),
    sync_status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    skip_reason          VARCHAR(255),
    sync_message         VARCHAR(1000),
    retry_count          INT           NOT NULL DEFAULT 0,
    version              INT           NOT NULL DEFAULT 0,
    last_sync_at         TIMESTAMP,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted           SMALLINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_oa_contract_attachment_sync PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_oa_contract_attachment_sync IS 'OA货物购销合同附件回传CRM中间记录表';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.id                   IS '内部主键';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.oa_form_main_id      IS 'OA 购销合同主表 formmain_5296.ID';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.oa_start_date        IS 'OA 流程发起时间 formmain_5296.START_DATE';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.flow_status          IS '流程状态快照：已完成/未完成/未知';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.nc_order_no          IS 'NC 销售订单号 formmain_5296.FIELD0273';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.oa_contract_no       IS '销售订单编号 formmain_5296.FIELD0104';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.oa_attachment_ref    IS '合同附件引用 formmain_5296.FIELD0360';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.oa_attachment_id     IS 'OA 附件主键 ctp_attachment.ID';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.attachment_file_id   IS 'OA 附件文件标识 ctp_attachment.FILE_URL';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.attachment_file_name IS 'OA 附件文件名 ctp_attachment.FILENAME';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.attachment_url       IS '回传 CRM 的附件访问地址';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.crm_contract_id      IS 'CRM 合同数据 ID';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.sync_status          IS '同步状态：PENDING-待处理/PROCESSING-处理中/SUCCESS-成功/RETRY-失败待重试/SKIPPED-业务跳过';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.skip_reason          IS '业务跳过原因';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.sync_message         IS '同步说明或失败原因';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.retry_count          IS '累计失败次数';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.version              IS '乐观锁版本号';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.last_sync_at         IS '最近一次同步时间';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.created_at           IS '记录创建时间';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.updated_at           IS '记录更新时间';
COMMENT ON COLUMN t_integration_oa_contract_attachment_sync.is_deleted           IS '逻辑删除：0-正常 1-删除';

-- 幂等键：一个合同只有一个附件
-- 注意：UPSERT 时必须携带部分索引谓词，即
--   ON CONFLICT (oa_form_main_id) WHERE is_deleted = 0 DO UPDATE ...
CREATE UNIQUE INDEX IF NOT EXISTS uk_oa_contract_attachment_sync_form_main
    ON t_integration_oa_contract_attachment_sync (oa_form_main_id)
    WHERE is_deleted = 0;

-- 执行阶段捞取待处理数据
CREATE INDEX IF NOT EXISTS idx_oa_contract_attachment_sync_status
    ON t_integration_oa_contract_attachment_sync (sync_status, updated_at)
    WHERE is_deleted = 0;

-- 按 NC 销售订单号查询/补偿
CREATE INDEX IF NOT EXISTS idx_oa_contract_attachment_sync_nc
    ON t_integration_oa_contract_attachment_sync (nc_order_no)
    WHERE is_deleted = 0;
