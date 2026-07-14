package com.cacch.integration.integration.oa.support;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link OaResponseSupport} 单测
 *
 * @author hongfu_zhou@cacch.com
 */
class OaResponseSupportTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void extractProcessId_fromPlainString() {
        assertEquals("12345", OaResponseSupport.extractProcessId(MAPPER.getNodeFactory().textNode("12345")));
    }

    @Test
    void extractProcessId_fromNestedData() throws Exception {
        assertEquals("pid-9", OaResponseSupport.extractProcessId(
                MAPPER.readTree("{\"data\":{\"processId\":\"pid-9\"}}")));
    }

    @Test
    void extractProcessId_nullWhenMissing() throws Exception {
        assertNull(OaResponseSupport.extractProcessId(MAPPER.readTree("{\"code\":0}")));
    }
}
