package com.cacch.integration.common.enums.meeting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 会议记录状态
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum MeetingRecordStatusEnum {

    PENDING("PENDING", "待发起"),
    SCHEDULED("SCHEDULED", "已创建"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;
}
