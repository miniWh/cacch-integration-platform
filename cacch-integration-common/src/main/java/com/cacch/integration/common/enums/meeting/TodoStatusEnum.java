package com.cacch.integration.common.enums.meeting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 待办事项状态
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum TodoStatusEnum {

    PENDING("PENDING", "待办"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String desc;
}
