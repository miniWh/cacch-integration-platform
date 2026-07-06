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
     * 员工会议管理文档 — 会议子表名称
     */
    public static final String MEETING_SHEET_TITLE = "会议管理";

    /**
     * 员工会议管理文档 — 待办子表名称
     */
    public static final String TODO_SHEET_TITLE = "会议待办事项";

    /**
     * 自动建会所需最短会议时长（分钟）
     */
    public static final int MIN_MEETING_DURATION_MINUTES = 5;

    /**
     * 员工会议管理表列定义（有序，从左到右）。
     * 初始化时通过 add_fields 新建；因企微批量新增列会插入到左侧，实际 API 调用顺序需与此列表相反。
     */
    public static final List<MeetingSheetColumnDef> MEETING_SHEET_COLUMNS = List.of(
            new MeetingSheetColumnDef("meeting_title", "会议主题", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("meeting_description", "会议描述", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("start_time", "开始时间", WeComConstants.FIELD_TYPE_DATE_TIME),
            new MeetingSheetColumnDef("duration", "会议时长（分钟）", WeComConstants.FIELD_TYPE_NUMBER),
            new MeetingSheetColumnDef("attendees", "参会人", WeComConstants.FIELD_TYPE_USER),
            new MeetingSheetColumnDef("status", "会议状态", WeComConstants.FIELD_TYPE_SINGLE_SELECT,
                    meetingStatusOptions()),
            new MeetingSheetColumnDef("minutes_status", "纪要状态", WeComConstants.FIELD_TYPE_SINGLE_SELECT,
                    minutesStatusOptions()),
            new MeetingSheetColumnDef("meeting_link", "会议链接", WeComConstants.FIELD_TYPE_URL),
            new MeetingSheetColumnDef("wecom_meeting_code", "会议号", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("wecom_meeting_id", "会议ID", WeComConstants.FIELD_TYPE_TEXT)
    );

    /**
     * 会议待办事项子表列定义（有序，从左到右）。
     * 前三列类型与会议管理子表对应列一致；初始化时 API 调用顺序与此列表相反。
     */
    public static final List<MeetingSheetColumnDef> TODO_SHEET_COLUMNS = List.of(
            new MeetingSheetColumnDef("meeting_title", "会议主题", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("wecom_meeting_code", "会议号", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("start_time", "开始时间", WeComConstants.FIELD_TYPE_DATE_TIME),
            new MeetingSheetColumnDef("todo_item", "待办事项", WeComConstants.FIELD_TYPE_TEXT),
            new MeetingSheetColumnDef("assignee", "责任人", WeComConstants.FIELD_TYPE_USER)
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

    /**
     * 构建会议待办子表逻辑 key → 列标题 映射
     */
    public static Map<String, String> buildTodoColumnTitleMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (MeetingSheetColumnDef column : TODO_SHEET_COLUMNS) {
            mapping.put(column.logicalKey(), column.title());
        }
        return mapping;
    }
}
