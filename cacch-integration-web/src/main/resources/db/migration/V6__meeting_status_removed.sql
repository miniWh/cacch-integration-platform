-- =============================================
-- V6: 会议状态移除「进行中」「已结束」
--     智能表格「会议状态」单选列与本地枚举对齐为：待发起、已创建、已取消
-- =============================================

COMMENT ON COLUMN t_integration_meeting_record.status IS
    '会议状态: PENDING-待发起 SCHEDULED-已创建 CANCELLED-已取消';

-- 历史数据归并为「已创建」
UPDATE t_integration_meeting_record
SET status     = 'SCHEDULED',
    updated_at = CURRENT_TIMESTAMP
WHERE is_deleted = 0
  AND status IN ('IN_PROGRESS', 'COMPLETED');
