-- =============================================
-- V9: 总控表「申请人ID」改为人员列「申请人」，删除「申请人姓名」列映射
-- =============================================

COMMENT ON COLUMN t_integration_smart_table.meeting_column_mapping IS
    '主sheet列映射JSON，值为企微列标题（非 fieldId）。'
    'MASTER格式: {"applicant":"申请人","apply_status":"申请状态","created_doc_id":"已创建文档ID","created_doc_url":"已创建文档链接"} ；'
    'MEETING格式: {"meeting_title":"会议主题","meeting_type":"会议类型","meeting_topics":"会议议题","meeting_description":"会议描述","location":"地点","start_time":"开始时间","duration":"会议时长（分钟）","attendees":"参会人","status":"会议状态","minutes_status":"纪要状态","meeting_link":"会议链接","wecom_meeting_code":"会议号","wecom_meeting_id":"会议ID"}';

UPDATE t_integration_smart_table
SET meeting_column_mapping = '{
  "applicant": "申请人",
  "apply_status": "申请状态",
  "created_doc_id": "已创建文档ID",
  "created_doc_url": "已创建文档链接"
}'::jsonb,
    updated_at             = CURRENT_TIMESTAMP
WHERE table_type = 'MASTER'
  AND is_deleted = 0;
