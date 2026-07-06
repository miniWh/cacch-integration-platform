package com.cacch.integration.common.enums.meeting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 智能表格类型
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum SmartTableTypeEnum {

    MASTER("MASTER", "总控申请表"),
    MEETING("MEETING", "员工会议管理表");

    private final String code;
    private final String desc;
}
