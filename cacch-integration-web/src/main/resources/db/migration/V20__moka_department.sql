-- =============================================
-- V20: Moka 组织架构部门表（主表 + 多语言子表）
-- =============================================

-- 部门主表：以客户系统 department_code 为主键
CREATE TABLE IF NOT EXISTS moka_department (
    department_code VARCHAR(500) NOT NULL PRIMARY KEY,
    name            VARCHAR(1000) NOT NULL,
    parent_code      VARCHAR(500) NOT NULL,
    type            SMALLINT,
    sequence        NUMERIC(10, 2),
    operator_email  VARCHAR(255),
    create_time     TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  moka_department                IS 'Moka组织架构部门主表';
COMMENT ON COLUMN moka_department.department_code IS '客户系统的部门id（主键）';
COMMENT ON COLUMN moka_department.name            IS '部门名称';
COMMENT ON COLUMN moka_department.parent_code     IS '上级部门唯一id，一级部门传"0"';
COMMENT ON COLUMN moka_department.type            IS '部门类型：1-普通部门（默认），2-门店部门';
COMMENT ON COLUMN moka_department.sequence         IS '部门排序，支持0~10000两位小数，为空默认排在最后';
COMMENT ON COLUMN moka_department.operator_email   IS '系统内操作人邮箱（用于记录日志）';
COMMENT ON COLUMN moka_department.create_time     IS '记录创建时间';

-- 多语言子表：外键关联主表，on delete cascade 级联删除
CREATE TABLE IF NOT EXISTS moka_department_localized (
    department_code VARCHAR(500) NOT NULL,
    locale          VARCHAR(50)  NOT NULL,
    prop_value      VARCHAR(1000),
    CONSTRAINT pk_moka_dept_localized PRIMARY KEY (department_code, locale),
    CONSTRAINT fk_moka_dept_localized FOREIGN KEY (department_code)
        REFERENCES moka_department (department_code) ON DELETE CASCADE
);

COMMENT ON TABLE  moka_department_localized                IS 'Moka部门多语言名称子表';
COMMENT ON COLUMN moka_department_localized.department_code IS '关联部门ID（主键之一 + 外键）';
COMMENT ON COLUMN moka_department_localized.locale          IS '语言代码（如 zh_CN / en_US）';
COMMENT ON COLUMN moka_department_localized.prop_value      IS '该语言对应的部门名称';

-- 索引：按父部门查询子部门、按语言过滤
CREATE INDEX IF NOT EXISTS idx_moka_dept_parent     ON moka_department(parent_code);
CREATE INDEX IF NOT EXISTS idx_moka_dept_localized  ON moka_department_localized(locale);
