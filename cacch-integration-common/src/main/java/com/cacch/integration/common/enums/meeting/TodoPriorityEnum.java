package com.cacch.integration.common.enums.meeting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 待办优先级
 */
@Getter
@RequiredArgsConstructor
public enum TodoPriorityEnum {

    HIGH("HIGH", "高"),
    MEDIUM("MEDIUM", "中"),
    LOW("LOW", "低");

    private final String code;
    private final String desc;
}
