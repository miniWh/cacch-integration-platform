-- =============================================
-- V42: 部门主数据来源标识抽离 —— 新建 mdm_dept_src_ref 多源映射表
--   1) 新建 mdm_dept_src_ref（镜像 mdm_emp_src_ref 模式）
--   2) 存量回填：mdm_dept.dept_uuid → src_ref（IHR / DEPT）
--   3) 删除 mdm_dept.dept_uuid 列（主档只保留主数据本体）
--   部署顺序约束：本脚本必须先于依赖 src_ref 的新代码上线执行
-- =============================================

-- ---------- 1. mdm_dept_src_ref 部门主数据多源映射表 ----------
CREATE TABLE IF NOT EXISTS mdm_dept_src_ref (
    id               BIGINT       NOT NULL,     -- 内部主键（雪花生成）
    dept_id          BIGINT       NOT NULL,     -- 关联 mdm_dept.id
    src_sys_code     VARCHAR(32)  NOT NULL,     -- 来源系统编码：IHR / WECOM / OA 等
    src_entity_type  VARCHAR(32)  NOT NULL,     -- 源系统实体类型：DEPT
    src_entity_id    VARCHAR(128) NOT NULL,     -- 源系统实体 ID（iHR 部门 uuid）
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_mdm_dept_src_ref PRIMARY KEY (id),
    CONSTRAINT uk_mdm_dept_src_ref_entity UNIQUE (src_sys_code, src_entity_type, src_entity_id),
    CONSTRAINT fk_mdm_dept_src_ref_dept FOREIGN KEY (dept_id) REFERENCES mdm_dept (id)
);

COMMENT ON TABLE  mdm_dept_src_ref                IS '部门主数据多源映射表';
COMMENT ON COLUMN mdm_dept_src_ref.id             IS '内部主键';
COMMENT ON COLUMN mdm_dept_src_ref.dept_id        IS '关联 mdm_dept.id';
COMMENT ON COLUMN mdm_dept_src_ref.src_sys_code   IS '来源系统编码：IHR / WECOM / OA 等';
COMMENT ON COLUMN mdm_dept_src_ref.src_entity_type IS '源系统实体类型：DEPT';
COMMENT ON COLUMN mdm_dept_src_ref.src_entity_id  IS '源系统实体 ID（iHR 部门 uuid）';
COMMENT ON COLUMN mdm_dept_src_ref.created_at     IS '记录创建时间';
COMMENT ON COLUMN mdm_dept_src_ref.updated_at     IS '记录更新时间';
COMMENT ON COLUMN mdm_dept_src_ref.is_deleted     IS '逻辑删除：0-正常 1-删除';

CREATE INDEX IF NOT EXISTS idx_mdm_dept_src_ref_dept_id
    ON mdm_dept_src_ref(dept_id);

-- ---------- 3. 删除 mdm_dept.dept_uuid（含唯一约束） ----------
ALTER TABLE mdm_dept DROP CONSTRAINT IF EXISTS uk_mdm_dept_uuid;
ALTER TABLE mdm_dept DROP COLUMN IF EXISTS dept_uuid;
