package com.cacch.integration.integration.oa.client.dto;

import com.cacch.integration.integration.oa.support.OaResponseSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CAP4 batch-update 请求与响应解析单元测试
 *
 * @author hongfu_zhou@cacch.com
 */
class OaCap4BatchUpdateSupportTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void regReportAttachmentBind_shouldIncludeAttachmentInfos() {
        Map<String, Object> body = OaCap4BatchUpdateRequest.regReportAttachmentBind(
                "REG_FORM_CODE",
                "zhouhongfu",
                "56195256829429332.-470190193844795028",
                false,
                "formmain_4070",
                5185606166217772201L,
                "formson_5464",
                8703187152583019529L,
                "field0218",
                "8451540374587001174",
                "-7390855572027915259",
                1);

        assertEquals("REG_FORM_CODE", body.get("formCode"));
        assertEquals("zhouhongfu", body.get("loginName"));
        assertEquals(false, body.get("doTrigger"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) body.get("dataList");
        Map<String, Object> dataItem = dataList.getFirst();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attachmentInfos = (List<Map<String, Object>>) dataItem.get("attachmentInfos");
        Map<String, Object> attachmentInfo = attachmentInfos.getFirst();
        assertEquals("8451540374587001174", attachmentInfo.get("subReference"));
        assertEquals("-7390855572027915259", attachmentInfo.get("fileUrl"));
        assertEquals(1, attachmentInfo.get("sort"));
    }

    @Test
    void isCap4BatchUpdateSuccess_shouldDetectSuccessAndFailure() throws Exception {
        assertTrue(OaResponseSupport.isCap4BatchUpdateSuccess(MAPPER.readTree("""
                {"code":0,"data":{"successCount":1,"failedCount":0},"message":""}
                """)));
        assertFalse(OaResponseSupport.isCap4BatchUpdateSuccess(MAPPER.readTree("""
                {"code":0,"data":{"successCount":0,"failedCount":1,"failedData":{"5185606166217772201":"无权限"}},"message":""}
                """)));
        assertFalse(OaResponseSupport.isCap4BatchUpdateSuccess(MAPPER.readTree("""
                {"code":500,"message":"error"}
                """)));
    }
}
