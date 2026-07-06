-- =============================================
-- V5: 会议待办事项子表列结构标准化
--     待办子表列：会议主题、企微会议号、开始时间、待办事项、责任人
-- =============================================

COMMENT ON COLUMN t_integration_smart_table.todo_sheet_id IS
    '待办子表ID（会议待办事项），MASTER时为NULL';

COMMENT ON COLUMN t_integration_smart_table.todo_column_mapping IS
    '待办子表列映射JSON，值为企微列标题（非 fieldId），格式: '
    '{"meeting_title":"会议主题","wecom_meeting_code":"企微会议号","start_time":"开始时间","todo_item":"待办事项","assignee":"责任人"}';

UPDATE t_integration_smart_table
SET todo_column_mapping = '{
  "meeting_title": "会议主题",
  "wecom_meeting_code": "企微会议号",
  "start_time": "开始时间",
  "todo_item": "待办事项",
  "assignee": "责任人"
}'::jsonb,
    updated_at          = CURRENT_TIMESTAMP
WHERE table_type = 'MEETING'
  AND is_deleted = 0
  AND todo_sheet_id IS NOT NULL;
