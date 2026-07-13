package com.cacch.integration.integration.crm.support;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link CrmEmployeeSupport} 单测
 *
 * @author hongfu_zhou@cacch.com
 */
class CrmEmployeeSupportTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void firstEmployeeNode_fromObject() throws Exception {
        JsonNode node = MAPPER.readTree("{\"id\":\"E1\",\"emp_code\":\"u001\",\"emp_name\":\"张三\"}");
        JsonNode employee = CrmEmployeeSupport.firstEmployeeNode(node);
        assertNotNull(employee);
        assertEquals("u001", CrmEmployeeSupport.text(employee, "emp_code"));
        assertEquals("张三", CrmEmployeeSupport.text(employee, "emp_name"));
    }

    @Test
    void firstEmployeeNode_fromArray() throws Exception {
        JsonNode node = MAPPER.readTree("[{\"emp_code\":\"a\"},{\"emp_code\":\"b\"}]");
        assertEquals("a", CrmEmployeeSupport.text(CrmEmployeeSupport.firstEmployeeNode(node), "emp_code"));
    }

    @Test
    void firstEmployeeNode_fromWrappedData() throws Exception {
        JsonNode node = MAPPER.readTree("{\"data\":{\"emp_code\":\"c1\"}}");
        assertEquals("c1", CrmEmployeeSupport.text(CrmEmployeeSupport.firstEmployeeNode(node), "emp_code"));
    }

    @Test
    void firstEmployeeNode_nullSafe() {
        assertNull(CrmEmployeeSupport.firstEmployeeNode(null));
        assertNull(CrmEmployeeSupport.text(null, "emp_code"));
    }
}
