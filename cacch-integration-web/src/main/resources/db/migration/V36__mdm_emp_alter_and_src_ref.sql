-- =============================================
-- V36: mdm_emp 字段补齐 + 删除 id_hash + 新建 src_ref 映射表
--   前置: V35 已在所有环境执行（不可修改）
--   变更:
--     - mdm_emp: 删 id_hash / uk_mdm_emp_id_hash
--                补 personal_email / work_email / id_card_type / id_card_hash / leave_date / probation_end_date
--     - 新建 mdm_emp_src_ref: 源系统 handle ↔ md_code 多对一映射
-- =============================================

-- ---------- 1. mdm_emp 删 id_hash（已上线，需 ALTER） ----------
-- 先删 UNIQUE 约束再删列（PostgreSQL 12 必须顺序操作）
ALTER TABLE mdm_emp DROP CONSTRAINT IF EXISTS uk_mdm_emp_id_hash;
ALTER TABLE mdm_emp DROP COLUMN IF EXISTS id_hash;
-- 删除遗留 COMMENT
COMMENT ON COLUMN mdm_emp.id_hash IS NULL;

-- ---------- 2. mdm_emp 补 6 个业务字段 ----------
ALTER TABLE mdm_emp ADD COLUMN IF NOT EXISTS personal_email      VARCHAR(128);  -- 个人邮箱（原 email 字段改名为 work_email）
ALTER TABLE mdm_emp ADD COLUMN IF NOT EXISTS work_email          VARCHAR(128);  -- 工作邮箱（主用邮箱）
ALTER TABLE mdm_emp ADD COLUMN IF NOT EXISTS id_card_type        VARCHAR(32);   -- 证件类型：IDENTITY_CARD / CHINA_PASSPORT 等
ALTER TABLE mdm_emp ADD COLUMN IF NOT EXISTS id_card            VARCHAR(64);   -- 证件号（明文，身份证号等；脱敏输出走查询侧）
ALTER TABLE mdm_emp ADD COLUMN IF NOT EXISTS leave_date          DATE;          -- 当前离职日期快照（任职表拉链承载历史）
ALTER TABLE mdm_emp ADD COLUMN IF NOT EXISTS probation_end_date  DATE;          -- 当前试用结束日期快照

COMMENT ON COLUMN mdm_emp.personal_email     IS '个人邮箱';
COMMENT ON COLUMN mdm_emp.work_email         IS '工作邮箱';
COMMENT ON COLUMN mdm_emp.id_card_type        IS '证件类型：IDENTITY_CARD / CHINA_PASSPORT / FOREIGN_PASSPORT / MTP / MTPFHKAMR / OTHER';
COMMENT ON COLUMN mdm_emp.id_card            IS '证件号';
COMMENT ON COLUMN mdm_emp.leave_date          IS '当前离职日期快照';
COMMENT ON COLUMN mdm_emp.probation_end_date IS '当前试用结束日期快照';

-- ---------- 3. mdm_emp_src_ref 多源映射表 ----------
-- 用途：源系统 handle ↔ mdm_emp.md_code 多对一映射（一人多源场景）
-- 匹配主档逻辑：ingest 时先查 src_ref 拿 md_code，无则 INSERT 新主档 + src_ref
CREATE TABLE IF NOT EXISTS mdm_emp_src_ref (
    id               BIGINT       NOT NULL,     -- 内部主键（雪花生成）
    md_code          BIGINT       NOT NULL,     -- 人员主数据编码（关联 mdm_emp.md_code）
    src_sys_code     VARCHAR(32)  NOT NULL,     -- 来源系统：IHR / WECOM / OA ...
    src_entity_type  VARCHAR(32)  NOT NULL,     -- 源系统实体类型：STAFF / USER / ACCOUNT
    src_entity_id    VARCHAR(128) NOT NULL,     -- 源系统实体 ID（iHR staffId UUID / 企微 userId / OA account）
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_mdm_emp_src_ref PRIMARY KEY (id),
    CONSTRAINT uk_mdm_emp_src_ref_entity UNIQUE (src_sys_code, src_entity_type, src_entity_id)
);

COMMENT ON TABLE  mdm_emp_src_ref                 IS '人员主数据多源映射表';
COMMENT ON COLUMN mdm_emp_src_ref.id              IS '内部主键';
COMMENT ON COLUMN mdm_emp_src_ref.md_code         IS '人员主数据编码';
COMMENT ON COLUMN mdm_emp_src_ref.src_sys_code     IS '来源系统编码：IHR / WECOM / OA 等';
COMMENT ON COLUMN mdm_emp_src_ref.src_entity_type  IS '源系统实体类型：STAFF / USER / ACCOUNT';
COMMENT ON COLUMN mdm_emp_src_ref.src_entity_id    IS '源系统实体 ID';
COMMENT ON COLUMN mdm_emp_src_ref.created_at      IS '记录创建时间';
COMMENT ON COLUMN mdm_emp_src_ref.updated_at      IS '记录更新时间';
COMMENT ON COLUMN mdm_emp_src_ref.is_deleted      IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_mdm_emp_src_ref_md_code
    ON mdm_emp_src_ref(md_code);
