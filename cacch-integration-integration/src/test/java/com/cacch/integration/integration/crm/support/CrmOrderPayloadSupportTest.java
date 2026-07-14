package com.cacch.integration.integration.crm.support;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link CrmOrderPayloadSupport} 单测
 *
 * @author hongfu_zhou@cacch.com
 */
class CrmOrderPayloadSupportTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void listRecords_fromRows() throws Exception {
        JsonNode data = MAPPER.readTree("{\"rows\":[{\"id\":\"1\",\"name\":\"NO1\"},{\"id\":\"2\"}]}");
        List<JsonNode> list = CrmOrderPayloadSupport.listRecords(data);
        assertEquals(2, list.size());
        assertEquals("NO1", CrmOrderPayloadSupport.text(list.get(0), "name"));
    }

    @Test
    void nestedText_customer() throws Exception {
        JsonNode node = MAPPER.readTree("{\"customer\":{\"id\":\"C1\",\"name\":\"客户A\"}}");
        assertEquals("C1", CrmOrderPayloadSupport.nestedText(node, "customer", "id"));
        assertEquals("客户A", CrmOrderPayloadSupport.nestedText(node, "customer", "name"));
    }

    @Test
    void parseCrmDateTime_millis() {
        assertNotNull(CrmOrderPayloadSupport.parseCrmDateTime(
                MAPPER.getNodeFactory().textNode("1719763200000")));
    }
}
