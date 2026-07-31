package com.cacch.integration.integration.oa.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link OaRegReportPathSupport} 单元测试
 */
class OaRegReportPathSupportTest {

    @Test
    void formatL3DirectoryName_joinsSeqAndItemNameWith顿号() {
        assertEquals("1、农药登记申请表", OaRegReportPathSupport.formatL3DirectoryName("1", "农药登记申请表"));
        assertEquals("10、产品概述", OaRegReportPathSupport.formatL3DirectoryName("10", "产品概述"));
    }

    @Test
    void formatL3DirectoryName_blankSeqReturnsItemNameOnly() {
        assertEquals("农药登记申请表", OaRegReportPathSupport.formatL3DirectoryName(null, "农药登记申请表"));
        assertEquals("农药登记申请表", OaRegReportPathSupport.formatL3DirectoryName("", "农药登记申请表"));
    }
}
