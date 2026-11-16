-- =============================================
-- V44: mdm_position.department_id 切换为 mdm_dept.id（BIGINT）
--
--   各层 department_id 职责（分层单一原则）：
--     stg_ihr_position.raw_json             → 贴源原文，含完整 iHR 报文
--     ods_ihr_position.department_id        → VARCHAR(64) 保持不动，存 iHR 原始 Long 型部门 ID
--                                              （ODS 职责：完整源系统快照，不持有主数据标识）
--     mdm_position.department_id            → BIGINT 存 mdm_dept.id（翻译结果）
--                                              （主档职责：纯主数据语义，所有引用都是主数据侧标识）
--     mdm_position.department_ihr_id        → 不新增（冗余，ODS 已有；多源追溯走 src_ref）
--     mdm_position_src_ref                  → 只存职位 uuid → mdm_position.id，不存部门维度
--
--   本次变更范围：
--     0. 清空 V43 岗位链路四表（字段类型变更 + 翻译逻辑，旧数据无意义）
--     1. mdm_position.department_id VARCHAR(64) → BIGINT
--     2. 更新 COMMENT 说明分层职责；ODS 字段类型保持不变，仅更新 COMMENT
--
--   翻译路径（由 Service 层 translateDeptId 实现，DB 脚本不包含逻辑）:
--     iHR departmentId(Long) → ods_ihr_dept.ihr_dept_id(String) → ihr_dept_uuid
--     → mdm_dept_src_ref(src_sys_code=IHR, src_entity_type=DEPT, src_entity_id=ihr_dept_uuid)
--     → mdm_dept.id
--   翻译失败置 null，岗位落库降级不阻断。
-- =============================================

-- ---------- 0. 清空 V43 岗位链路产生的全量数据（字段类型变更 + 翻译逻辑，旧数据无意义） ----------
TRUNCATE TABLE mdm_position_src_ref,
             mdm_position_event,
             mdm_position,
             ods_ihr_position,
             stg_ihr_position
CASCADE;

-- ---------- 1. mdm_position: department_id VARCHAR(64) → BIGINT ----------

DROP INDEX IF EXISTS idx_mdm_position_dept;

ALTER TABLE mdm_position
    ALTER COLUMN department_id TYPE BIGINT
        USING CASE WHEN department_id ~ '^\d+$' THEN department_id::BIGINT ELSE NULL END;

COMMENT ON COLUMN mdm_position.department_id IS '所属部门ID';

CREATE INDEX idx_mdm_position_dept
    ON mdm_position(department_id) WHERE department_id IS NOT NULL AND is_deleted = 0;

-- ---------- 2. ods_ihr_position.department_id 保持 VARCHAR(64)，仅更新 COMMENT ----------

COMMENT ON COLUMN ods_ihr_position.department_id IS 'iHR 部门ID';
