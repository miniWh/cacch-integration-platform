-- =============================================
-- V17: 法大大个人实名认证记录表
-- =============================================

CREATE TABLE IF NOT EXISTS t_integration_fdd_person_auth (
    id                      BIGINT       NOT NULL,
    internal_company_name   VARCHAR(256) NOT NULL,
    transaction_no          VARCHAR(64),
    person_name             VARCHAR(64)  NOT NULL,
    id_number               VARCHAR(18)  NOT NULL,
    mobile                  VARCHAR(11),
    auth_url                VARCHAR(1024),
    auth_status             VARCHAR(16)  NOT NULL,
    request_detail          JSONB,
    auth_detail             JSONB,
    fail_reason             VARCHAR(512),
    source_system           VARCHAR(16)  NOT NULL,
    source_biz_no           VARCHAR(128),
    certified_at            TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_fdd_person_auth PRIMARY KEY (id)
);

COMMENT ON TABLE  t_integration_fdd_person_auth IS '法大大个人实名认证记录';
COMMENT ON COLUMN t_integration_fdd_person_auth.id IS '雪花主键';
COMMENT ON COLUMN t_integration_fdd_person_auth.internal_company_name IS '内部企业全称（业务判定键之一）';
COMMENT ON COLUMN t_integration_fdd_person_auth.transaction_no IS '法大大认证流水号（回调匹配）';
COMMENT ON COLUMN t_integration_fdd_person_auth.person_name IS '姓名';
COMMENT ON COLUMN t_integration_fdd_person_auth.id_number IS '身份证号（业务判定键之一，明文）';
COMMENT ON COLUMN t_integration_fdd_person_auth.mobile IS '手机号（三要素，明文）';
COMMENT ON COLUMN t_integration_fdd_person_auth.auth_url IS '法大大认证页面 URL';
COMMENT ON COLUMN t_integration_fdd_person_auth.auth_status IS '认证状态：PENDING / SUCCESS / FAILED';
COMMENT ON COLUMN t_integration_fdd_person_auth.request_detail IS '发起认证时请求/响应原始报文';
COMMENT ON COLUMN t_integration_fdd_person_auth.auth_detail IS '回调原始报文';
COMMENT ON COLUMN t_integration_fdd_person_auth.fail_reason IS '失败原因';
COMMENT ON COLUMN t_integration_fdd_person_auth.source_system IS '发起来源系统（审计）：CRM / OA';
COMMENT ON COLUMN t_integration_fdd_person_auth.source_biz_no IS '来源系统业务单号';
COMMENT ON COLUMN t_integration_fdd_person_auth.certified_at IS '认证通过时间';
COMMENT ON COLUMN t_integration_fdd_person_auth.created_at IS '创建时间';
COMMENT ON COLUMN t_integration_fdd_person_auth.updated_at IS '更新时间';
COMMENT ON COLUMN t_integration_fdd_person_auth.is_deleted IS '逻辑删除：0正常 1删除';

-- 同一内部企业 + 身份证号 仅允许一条 SUCCESS
CREATE UNIQUE INDEX IF NOT EXISTS uk_fdd_person_auth_success
    ON t_integration_fdd_person_auth (internal_company_name, id_number)
    WHERE is_deleted = 0 AND auth_status = 'SUCCESS';

CREATE INDEX IF NOT EXISTS idx_fdd_person_auth_txn
    ON t_integration_fdd_person_auth (transaction_no)
    WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_fdd_person_auth_biz_key
    ON t_integration_fdd_person_auth (internal_company_name, id_number, auth_status, created_at DESC)
    WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_fdd_person_auth_company
    ON t_integration_fdd_person_auth (internal_company_name, created_at)
    WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_fdd_person_auth_status
    ON t_integration_fdd_person_auth (auth_status, created_at)
    WHERE is_deleted = 0;
