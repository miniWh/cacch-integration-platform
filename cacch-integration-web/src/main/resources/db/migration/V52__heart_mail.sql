-- =============================================
-- V52: Heart 邮件数据表（REQ-HEART-001 F1/F2）
--   用途：保存 IMAP 拉取的邮件正文与 PDF 附件元信息，
--        供后续邮件→PO 解析环节消费。
--   幂等键：mail_key（Message-ID，缺失时归一化为 UID:{mail_uid}）
--   流程状态：MAIL_RECEIVED → 解析；SKIP 非 Heart 邮件；
--            FAILED 拉取失败待重试；MANUAL_REVIEW 重试超次待人工
--   附件：attach_files 为 JSONB 数组，元素 {name, sha256, path, size}
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_heart_mail (
    id              BIGINT        NOT NULL,
    mail_key        VARCHAR(255)  NOT NULL,
    mail_uid        BIGINT,
    message_id      VARCHAR(255),
    subject         VARCHAR(500),
    from_address    VARCHAR(255),
    received_at     TIMESTAMP,
    body_text       TEXT,
    body_html       TEXT,
    attach_files    JSONB,
    process_status  VARCHAR(32)   NOT NULL DEFAULT 'MAIL_RECEIVED',
    fail_reason     VARCHAR(1000),
    retry_count     INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_heart_mail PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_heart_mail_key
    ON t_integration_heart_mail (mail_key) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_heart_mail_status
    ON t_integration_heart_mail (process_status, retry_count);

CREATE INDEX IF NOT EXISTS idx_heart_mail_received
    ON t_integration_heart_mail (received_at);

COMMENT ON TABLE t_integration_heart_mail IS 'Heart 客户 PO 来源邮件';
COMMENT ON COLUMN t_integration_heart_mail.mail_key IS '去重键：Message-ID，缺失时为 UID:{mail_uid}';
COMMENT ON COLUMN t_integration_heart_mail.mail_uid IS 'IMAP 邮件 UID';
COMMENT ON COLUMN t_integration_heart_mail.message_id IS '邮件原始 Message-ID';
COMMENT ON COLUMN t_integration_heart_mail.subject IS '邮件主题';
COMMENT ON COLUMN t_integration_heart_mail.from_address IS '发件人地址';
COMMENT ON COLUMN t_integration_heart_mail.received_at IS '收件时间';
COMMENT ON COLUMN t_integration_heart_mail.body_text IS '纯文本正文（超长截断）';
COMMENT ON COLUMN t_integration_heart_mail.body_html IS 'HTML 正文（超长截断）';
COMMENT ON COLUMN t_integration_heart_mail.attach_files IS 'PDF 附件数组';
COMMENT ON COLUMN t_integration_heart_mail.process_status IS '流程状态：MAIL_RECEIVED/SKIP/FAILED/MANUAL_REVIEW';
COMMENT ON COLUMN t_integration_heart_mail.fail_reason IS '失败原因';
COMMENT ON COLUMN t_integration_heart_mail.retry_count IS '累计重试次数';
