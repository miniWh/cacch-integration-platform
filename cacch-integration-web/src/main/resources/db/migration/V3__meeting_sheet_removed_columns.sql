-- =============================================
-- V3: 员工会议智能表格列结构调整
--     移除企微智能表格中已废弃的列，并同步清理本地冗余字段与列映射
--
-- 移除的智能表格列（MEETING 子表）:
--   - meeting_date   会议日期（合并入「开始时间」日期时间列）
--   - minutes        会议纪要（改为纪要状态 minutes_status 单选列）
--   - reminder_sent  会前提醒是否已发送（业务不再在表格中维护）
--
-- 说明:
--   t_integration_meeting_record.meeting_date 仍保留，
--   由智能表格「开始时间」列解析后写入，供建会等内部逻辑使用。
-- =============================================

-- ---------------------------------------------
-- 1. 删除会议记录表中已废弃字段
-- ---------------------------------------------
ALTER TABLE t_integration_meeting_record
    DROP COLUMN IF EXISTS reminder_sent;

-- ---------------------------------------------
-- 2. 更新智能表格列映射说明（MEETING 新格式）
-- ---------------------------------------------
COMMENT ON COLUMN t_integration_smart_table.meeting_column_mapping IS
    '主sheet列映射JSON，MASTER格式: {"user_id":"fld_xxx","user_name":"fld_xxx","apply_status":"fld_xxx","created_doc_id":"fld_xxx","created_doc_url":"fld_xxx"} ；'
    'MEETING格式: {"meeting_title":"fld_xxx","start_time":"fld_xxx","duration":"fld_xxx","attendees":"fld_xxx","status":"fld_xxx","minutes_status":"fld_xxx","meeting_link":"fld_xxx","wecom_meeting_code":"fld_xxx","wecom_meeting_id":"fld_xxx"}';

-- ---------------------------------------------
-- 3. 更新会议记录字段注释（与智能表格列对齐）
-- ---------------------------------------------
COMMENT ON COLUMN t_integration_meeting_record.meeting_date IS
    '会议日期（由智能表格「开始时间」日期时间列解析，非独立表格列）';
COMMENT ON COLUMN t_integration_meeting_record.start_time IS
    '会议开始时刻（由智能表格「开始时间」日期时间列解析）';
COMMENT ON COLUMN t_integration_meeting_record.minutes_status IS
    '纪要状态，对应智能表格「纪要状态」单选列: NONE-无 PENDING-待解析 GENERATED-已生成';

-- ---------------------------------------------
-- 4. 清理 MEETING 配置中已废弃的列映射 key
-- ---------------------------------------------
UPDATE t_integration_smart_table
SET meeting_column_mapping = meeting_column_mapping - 'meeting_date' - 'minutes' - 'reminder_sent',
    updated_at             = CURRENT_TIMESTAMP
WHERE table_type = 'MEETING'
  AND is_deleted = 0
  AND (
    meeting_column_mapping ? 'meeting_date'
        OR meeting_column_mapping ? 'minutes'
        OR meeting_column_mapping ? 'reminder_sent'
    );
