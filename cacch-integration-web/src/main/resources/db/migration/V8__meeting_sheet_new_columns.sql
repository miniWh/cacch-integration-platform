-- =============================================
-- V8: 会议管理子表新增列：会议类型、会议议题、地点
-- =============================================

ALTER TABLE t_integration_meeting_record
    ADD COLUMN IF NOT EXISTS meeting_type   JSONB,
    ADD COLUMN IF NOT EXISTS meeting_topics JSONB,
    ADD COLUMN IF NOT EXISTS location       VARCHAR(500);

COMMENT ON COLUMN t_integration_meeting_record.meeting_type IS
    '会议类型（多选），对应智能表格「会议类型」列，不参与建会接口';
COMMENT ON COLUMN t_integration_meeting_record.meeting_topics IS
    '会议议题（多选），对应智能表格「会议议题」列，不参与建会接口';
COMMENT ON COLUMN t_integration_meeting_record.location IS
    '会议地点，对应智能表格「地点」列，创建企微会议时写入 location 参数';

COMMENT ON COLUMN t_integration_smart_table.meeting_column_mapping IS
    '主sheet列映射JSON，值为企微列标题（非 fieldId）。'
    'MASTER格式: {"user_id":"申请人ID","user_name":"姓名","apply_status":"申请状态","created_doc_id":"已创建文档ID","created_doc_url":"已创建文档链接"} ；'
    'MEETING格式: {"meeting_title":"会议主题","meeting_type":"会议类型","meeting_topics":"会议议题","meeting_description":"会议描述","location":"地点","start_time":"开始时间","duration":"会议时长（分钟）","attendees":"参会人","status":"会议状态","minutes_status":"纪要状态","meeting_link":"会议链接","wecom_meeting_code":"会议号","wecom_meeting_id":"会议ID"}';

UPDATE t_integration_smart_table
SET meeting_column_mapping = '{
  "meeting_title": "会议主题",
  "meeting_type": "会议类型",
  "meeting_topics": "会议议题",
  "meeting_description": "会议描述",
  "location": "地点",
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
