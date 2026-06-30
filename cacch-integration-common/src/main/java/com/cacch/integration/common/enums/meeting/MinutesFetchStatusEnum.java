package com.cacch.integration.common.enums.meeting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 会议纪要拉取/解析状态（t_integration_meeting_minutes.status）
 */
@Getter
@RequiredArgsConstructor
public enum MinutesFetchStatusEnum {

    NOT_FETCHED(0, "未拉取"),
    RAW_FETCHED(1, "已拉取原文"),
    SUMMARY_GENERATED(2, "已生成摘要"),
    TODO_PARSED(3, "已解析待办");

    private final int code;
    private final String desc;
}
