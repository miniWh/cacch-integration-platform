package com.cacch.integration.manager.crm.support;

import com.cacch.integration.common.constant.crm.CrmOaFormConstants;
import com.cacch.integration.entity.crm.CrmOrderDO;
import com.cacch.integration.entity.crm.CrmOrderDetailDO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link CrmOaFormMappingSupport} 单测
 *
 * @author hongfu_zhou@cacch.com
 */
class CrmOaFormMappingSupportTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void buildFormMain_mapsCoreFields() throws Exception {
        CrmOrderDO order = new CrmOrderDO();
        order.setOrderNo("SO-001");
        order.setCustomerName("客户A");
        order.setRawPayload(MAPPER.readTree("""
                {
                  "name":"SO-001",
                  "field_FXfm3__c":"NC-9",
                  "field_HVwgS__c":{"label":"上海泰禾"},
                  "customer":{"name":"客户A"},
                  "currency_type":{"label":"USD"},
                  "field_234i0__c":{"code":"1"},
                  "field_65xcf__c":{"label":"海运"},
                  "field_TZKmt__c":{"label":"电汇"},
                  "field_NjupX__c":{"name":"新加坡"},
                  "field_Z6A3J__c":{"name":"上海港"}
                }
                """));

        Map<String, Object> main = CrmOaFormMappingSupport.buildFormMain(order, "oa-user-1");
        assertEquals("NC-9", main.get("field0329"));
        assertEquals("上海泰禾", main.get("field0003"));
        assertEquals("oa-user-1", main.get("field0006"));
        assertEquals(CrmOaFormConstants.SALES_ORG, main.get("field0007"));
        assertEquals("客户A", main.get("field0008"));
        assertEquals("USD", main.get("field0009"));
        assertEquals("海运", main.get("field0012"));
        assertEquals("电汇", main.get("field0013"));
        assertEquals(CrmOaFormConstants.TRADE_TYPE, main.get("field0019"));
        assertEquals("1", main.get("field0029"));
        assertNull(main.get("field0222"));
        assertEquals("新加坡", main.get("field0461"));
        assertEquals("上海港", main.get("field0462"));
    }

    @Test
    void buildFormMain_emptyStringDefaultsWhenCrmBlank() throws Exception {
        CrmOrderDO order = new CrmOrderDO();
        order.setRawPayload(MAPPER.readTree("{\"name\":\"SO-002\"}"));

        Map<String, Object> main = CrmOaFormMappingSupport.buildFormMain(order, "oa-user-1");
        assertEquals(CrmOaFormConstants.EMPTY_STRING, main.get("field0329"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, main.get("field0012"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, main.get("field0013"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, main.get("field0015"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, main.get("field0017"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, main.get("field0018"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, main.get("field0029"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, main.get("field0462"));
    }

    @Test
    void buildFormSon_emptyStringDefaultsWhenCrmBlank() throws Exception {
        CrmOrderDO order = new CrmOrderDO();
        order.setRawPayload(MAPPER.readTree("{}"));
        CrmOrderDetailDO detail = new CrmOrderDetailDO();
        detail.setRawPayload(MAPPER.readTree("{}"));

        Map<String, Object> son = CrmOaFormMappingSupport.buildFormSon(order, detail);
        assertEquals(CrmOaFormConstants.EMPTY_STRING, son.get("field0074"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, son.get("field0089"));
        assertNull(son.get("field0091"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, son.get("field0092"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, son.get("field0093"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, son.get("field0129"));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, son.get("field0130"));
        assertEquals(CrmOaFormConstants.RETAIL_NO, son.get("field0443"));
    }

    @Test
    void buildFormSon_retailPackAndOrderDate() throws Exception {
        CrmOrderDO order = new CrmOrderDO();
        order.setRawPayload(MAPPER.readTree("{\"field_qx94q__c\":{\"value\":\"2026-08-01\"}}"));

        CrmOrderDetailDO detail = new CrmOrderDetailDO();
        detail.setPdCode("53001");
        detail.setPdCount("10");
        detail.setActualPrice("12.5");
        detail.setRawPayload(MAPPER.readTree("""
                {
                  "pd_count":"10",
                  "pd_code":"53001",
                  "actual_price":"12.5",
                  "field_Mb25P__c":"MAT-1",
                  "field_USMmk__c":"11"
                }
                """));

        Map<String, Object> son = CrmOaFormMappingSupport.buildFormSon(order, detail);
        assertEquals("10", son.get("field0074"));
        assertEquals("MAT-1", son.get("field0089"));
        assertNull(son.get("field0091"));
        assertEquals("12.5", son.get("field0092"));
        assertEquals("11", son.get("field0093"));
        assertEquals("", son.get("field0129"));
        assertEquals("2026-08-01", son.get("field0130"));
        assertEquals(CrmOaFormConstants.RETAIL_YES, son.get("field0443"));
    }

    @Test
    void mapRetailPack_non53() {
        assertEquals(CrmOaFormConstants.RETAIL_NO, CrmOaFormMappingSupport.mapRetailPack("99001"));
    }

    @Test
    void mapSealNeed_blankCodeReturnsEmptyString() {
        assertEquals(CrmOaFormConstants.EMPTY_STRING, CrmOaFormMappingSupport.mapSealNeed(null));
        assertEquals(CrmOaFormConstants.EMPTY_STRING, CrmOaFormMappingSupport.mapSealNeed(""));
        assertEquals("2", CrmOaFormMappingSupport.mapSealNeed("0"));
    }
}
