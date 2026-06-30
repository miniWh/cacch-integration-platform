package com.cacch.integration.common.enums.meeting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 会议记录状态
 */
@Getter
@RequiredArgsConstructor
public enum MeetingRecordStatusEnum {

    PENDING("PENDING", "待发起"),
    SCHEDULED("SCHEDULED", "已创建"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已结束"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;
}
