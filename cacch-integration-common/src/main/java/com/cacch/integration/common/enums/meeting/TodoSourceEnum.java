package com.cacch.integration.common.enums.meeting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 待办来源
 */
@Getter
@RequiredArgsConstructor
public enum TodoSourceEnum {

    FROM_MINUTES("FROM_MINUTES", "纪要解析"),
    MANUAL("MANUAL", "手动添加");

    private final String code;
    private final String desc;
}
