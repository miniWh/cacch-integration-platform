-- =============================================
-- V4: 智能表格列映射改为按列名称（field_title）存储与读写
--     企微 API key_type 使用 CELL_VALUE_KEY_TYPE_FIELD_TITLE，
--     用户删列重建后列名不变即可继续同步，无需更新 fieldId。
-- =============================================

-- ---------------------------------------------
-- 1. 更新列映射字段说明
-- ---------------------------------------------
COMMENT ON COLUMN t_integration_smart_table.meeting_column_mapping IS
    '主sheet列映射JSON，值为企微列标题（非 fieldId）。'
    'MASTER格式: {"user_id":"申请人ID","user_name":"姓名","apply_status":"申请状态","created_doc_id":"已创建文档ID","created_doc_url":"已创建文档链接"} ；'
    'MEETING格式: {"meeting_title":"会议主题","start_time":"开始时间","duration":"会议时长（分钟）","attendees":"参会人","status":"会议状态","minutes_status":"纪要状态","meeting_link":"会议链接","wecom_meeting_code":"企微会议号","wecom_meeting_id":"企微会议ID"}';

COMMENT ON COLUMN t_integration_smart_table.todo_column_mapping IS
    '待办sheet列映射JSON，值为企微列标题（非 fieldId），格式: {"todo_title":"待办标题","assignee":"负责人","due_date":"截止日期","priority":"优先级","status":"状态"}';

-- ---------------------------------------------
-- 2. MEETING 类型表：批量将已知逻辑 key 的映射值改为标准列标题
--    （历史 fieldId 值由运行时首次同步时按 get_fields 自动解析并回写）
-- ---------------------------------------------
UPDATE t_integration_smart_table
SET meeting_column_mapping = '{
  "meeting_title": "会议主题",
  "start_time": "开始时间",
  "duration": "会议时长（分钟）",
  "attendees": "参会人",
  "status": "会议状态",
  "minutes_status": "纪要状态",
  "meeting_link": "会议链接",
  "wecom_meeting_code": "企微会议号",
  "wecom_meeting_id": "企微会议ID"
}'::jsonb,
    updated_at             = CURRENT_TIMESTAMP
WHERE table_type = 'MEETING'
  AND is_deleted = 0;
