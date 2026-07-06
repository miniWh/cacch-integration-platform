package com.cacch.integration.common.constant.meeting;

/**
 * 员工会议管理智能表格列定义（逻辑 key → 列标题 → 企微字段类型）
 *
 * @author hongfu_zhou@cacch.com
 */
public record MeetingSheetColumnDef(String logicalKey, String title, String fieldType) {
}
