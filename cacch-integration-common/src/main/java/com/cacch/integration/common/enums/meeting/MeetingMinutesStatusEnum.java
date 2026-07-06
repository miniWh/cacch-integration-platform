package com.cacch.integration.common.enums.meeting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 会议记录上的纪要状态
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum MeetingMinutesStatusEnum {

    NONE("NONE", "无"),
    PENDING("PENDING", "待解析"),
    GENERATED("GENERATED", "已生成");

    private final String code;
    private final String desc;
}
