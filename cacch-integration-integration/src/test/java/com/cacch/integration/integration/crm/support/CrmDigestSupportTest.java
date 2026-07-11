package com.cacch.integration.integration.crm.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 勤策 digest 签名单测（官方示例）
 *
 * @author hongfu_zhou@cacch.com
 */
class CrmDigestSupportTest {

    @Test
    void digest_matchesOfficialExample() {
        String jsonBody = "{\"org_status\":\"1\"}";
        String appKey = "2EV5i30kYI6r7Hz817";
        String timestamp = "1617206400000";
        String digest = CrmDigestSupport.digest(jsonBody, appKey, timestamp);
        assertEquals("b7b7d9810bbb0786e9560751dc2b6597", digest);
    }
}
