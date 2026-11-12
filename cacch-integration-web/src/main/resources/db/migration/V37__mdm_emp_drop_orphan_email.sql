-- =============================================
-- V37: mdm_emp 清理孤儿 email 列
--   背景: V35 建表时 email VARCHAR(128) 存"优先工作邮箱"
--         V36 拆分 personal_email + work_email 后，
--         旧 email 列不再写入，存量数据可能有值、
--         新数据永远为 NULL，属于遗留孤儿列。
--   影响: 仅 DROP COLUMN，无业务逻辑变更
--         MdmEmpDO 已删 email 字段，无代码层依赖
-- =============================================

ALTER TABLE mdm_emp DROP COLUMN IF EXISTS email;
