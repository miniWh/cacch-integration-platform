package com.cacch.integration.integration.crm.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CrmTimeSupport} 单测
 *
 * @author hongfu_zhou@cacch.com
 */
class CrmTimeSupportTest {

    @Test
    void toEpochMilliString_fromDateTime() {
        // 2024-07-01 00:00:00 Asia/Shanghai = 1719763200000
        assertEquals("1719763200000",
                CrmTimeSupport.toEpochMilliString("2024-07-01 00:00:00", "beginTime"));
    }

    @Test
    void toEpochMilliString_fromMillis() {
        assertEquals("1719763200000",
                CrmTimeSupport.toEpochMilliString("1719763200000", "beginTime"));
    }

    @Test
    void toEpochMilliString_fromSeconds() {
        assertEquals("1719763200000",
                CrmTimeSupport.toEpochMilliString("1719763200", "beginTime"));
    }
}
