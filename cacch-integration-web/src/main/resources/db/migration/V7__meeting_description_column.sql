-- =============================================
-- V7: 会议管理子表新增「会议描述」列，会议号/会议ID 列名调整
-- =============================================

ALTER TABLE t_integration_meeting_record
    ADD COLUMN IF NOT EXISTS meeting_description TEXT;

COMMENT ON COLUMN t_integration_meeting_record.meeting_description IS
    '会议描述，对应智能表格「会议描述」文本列';
COMMENT ON COLUMN t_integration_meeting_record.wecom_meeting_id IS
    '会议ID（企微会议ID，系统自动创建后回写），对应智能表格「会议ID」列';
COMMENT ON COLUMN t_integration_meeting_record.wecom_meeting_code IS
    '会议号（企微会议号，系统自动创建后回写），对应智能表格「会议号」列';

COMMENT ON COLUMN t_integration_smart_table.meeting_column_mapping IS
    '主sheet列映射JSON，值为企微列标题（非 fieldId）。'
    'MASTER格式: {"user_id":"申请人ID","user_name":"姓名","apply_status":"申请状态","created_doc_id":"已创建文档ID","created_doc_url":"已创建文档链接"} ；'
    'MEETING格式: {"meeting_title":"会议主题","meeting_description":"会议描述","start_time":"开始时间","duration":"会议时长（分钟）","attendees":"参会人","status":"会议状态","minutes_status":"纪要状态","meeting_link":"会议链接","wecom_meeting_code":"会议号","wecom_meeting_id":"会议ID"}';

COMMENT ON COLUMN t_integration_smart_table.todo_column_mapping IS
    '待办子表列映射JSON，值为企微列标题（非 fieldId），格式: '
    '{"meeting_title":"会议主题","wecom_meeting_code":"会议号","start_time":"开始时间","todo_item":"待办事项","assignee":"责任人"}';

UPDATE t_integration_smart_table
SET meeting_column_mapping = '{
  "meeting_title": "会议主题",
  "meeting_description": "会议描述",
  "start_time": "开始时间",
  "duration": "会议时长（分钟）",
  "attendees": "参会人",
  "status": "会议状态",
  "minutes_status": "纪要状态",
  "meeting_link": "会议链接",
  "wecom_meeting_code": "会议号",
  "wecom_meeting_id": "会议ID"
}'::jsonb,
    updated_at             = CURRENT_TIMESTAMP
WHERE table_type = 'MEETING'
  AND is_deleted = 0;

UPDATE t_integration_smart_table
SET todo_column_mapping = jsonb_set(
        todo_column_mapping - 'wecom_meeting_code',
        '{wecom_meeting_code}',
        '"会议号"'::jsonb
    ),
    updated_at          = CURRENT_TIMESTAMP
WHERE table_type = 'MEETING'
  AND is_deleted = 0
  AND todo_sheet_id IS NOT NULL
  AND todo_column_mapping ? 'wecom_meeting_code';
