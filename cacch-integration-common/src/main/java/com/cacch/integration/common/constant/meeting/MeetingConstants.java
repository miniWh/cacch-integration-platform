package com.cacch.integration.common.constant.meeting;

import com.cacch.integration.common.constant.wecom.WeComConstants;

import java.util.List;

/**
 * 会议管理业务常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MeetingConstants {

    private MeetingConstants() {
    }

    /**
     * 总控表「申请状态」列 — 已批准（满足条件才创建员工会议管理表）
     */
    public static final String APPLY_STATUS_APPROVED = "已批准";

    /**
     * 智能表格配置默认启用状态
     */
    public static final int SMART_TABLE_STATUS_ENABLED = 1;

    /**
     * 员工会议管理表列定义（有序）。
     * 首列复用企微新建子表时的默认字段并重命名，其余列通过 add_fields 添加。
     */
    public static final List<MeetingSheetColumnDef> MEETING_SHEET_COLUMNS = List.of(
            new MeetingSheetColumnDef("meeting_title", "会议主题", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("meeting_date", "会议日期", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("start_time", "开始时间", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("duration", "会议时长(分钟)", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("attendees", "参会人", WeComConstants.FIELD_TYPE_USER),
            new MeetingSheetColumnDef("meeting_link", "会议链接", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("status", "状态", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("minutes", "会议纪要", WeComConstants.FIELD_TYPE_TEXT)
    );
}
