package com.cacch.integration.common.constant.meeting;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.common.enums.meeting.MeetingMinutesStatusEnum;
import com.cacch.integration.common.enums.meeting.MeetingRecordStatusEnum;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * 初始化时先删除子表全部原始列，再按此顺序通过 add_fields 新建。
     */
    public static final List<MeetingSheetColumnDef> MEETING_SHEET_COLUMNS = List.of(
            new MeetingSheetColumnDef("meeting_title", "会议主题", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("start_time", "开始时间", WeComConstants.FIELD_TYPE_DATE_TIME),
            new MeetingSheetColumnDef("duration", "会议时长（分钟）", WeComConstants.FIELD_TYPE_NUMBER),
            new MeetingSheetColumnDef("attendees", "参会人", WeComConstants.FIELD_TYPE_USER),
            new MeetingSheetColumnDef("status", "会议状态", WeComConstants.FIELD_TYPE_SINGLE_SELECT,
                    meetingStatusOptions()),
            new MeetingSheetColumnDef("minutes_status", "纪要状态", WeComConstants.FIELD_TYPE_SINGLE_SELECT,
                    minutesStatusOptions()),
            new MeetingSheetColumnDef("meeting_link", "会议链接", WeComConstants.FIELD_TYPE_URL),
            new MeetingSheetColumnDef("wecom_meeting_code", "企微会议号", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("wecom_meeting_id", "企微会议ID", WeComConstants.FIELD_TYPE_TEXT)
    );

    private static List<String> meetingStatusOptions() {
        return Arrays.stream(MeetingRecordStatusEnum.values())
                .map(MeetingRecordStatusEnum::getDesc)
                .toList();
    }

    private static List<String> minutesStatusOptions() {
        return Arrays.stream(MeetingMinutesStatusEnum.values())
                .map(MeetingMinutesStatusEnum::getDesc)
                .toList();
    }

    /**
     * 构建员工会议表逻辑 key → 列标题 映射（用于列初始化及 DB 存储）
     */
    public static Map<String, String> buildMeetingColumnTitleMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (MeetingSheetColumnDef column : MEETING_SHEET_COLUMNS) {
            mapping.put(column.logicalKey(), column.title());
        }
        return mapping;
    }
}
