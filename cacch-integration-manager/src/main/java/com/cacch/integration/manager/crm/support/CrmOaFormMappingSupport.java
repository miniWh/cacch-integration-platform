package com.cacch.integration.manager.crm.support;

import com.cacch.integration.common.constant.crm.CrmOaFormConstants;
import com.cacch.integration.entity.crm.CrmOrderDO;
import com.cacch.integration.entity.crm.CrmOrderDetailDO;
import com.cacch.integration.integration.crm.support.CrmOrderPayloadSupport;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRM 订单/明细 → OA formmain_2817 / formson_2819 字段映射（REQ-CRM-001 §6.4 / §6.5）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmOaFormMappingSupport {

    private CrmOaFormMappingSupport() {
    }

    /**
     * 组装主表字段
     *
     * @param order    订单主表 DO（含 raw_payload）
     * @param oaUserId 人员映射得到的 OA 人员 ID（field0006）
     * @return formmain_2817 字段 Map（含 null 公式字段）
     */
    public static Map<String, Object> buildFormMain(CrmOrderDO order, String oaUserId) {
        JsonNode raw = CrmOrderPayloadSupport.asJsonNode(order == null ? null : order.getRawPayload());
        Map<String, Object> formmain = new LinkedHashMap<>();

        // field0329  NC销售订单号 ← field_FXfm3__c
        formmain.put("field0329", firstNonBlank(
                CrmOrderPayloadSupport.text(raw, "field_FXfm3__c"),
                asPlainText(raw.get("field_FXfm3__c"))));
        // field0003  销售公司 ← field_HVwgS__c.label
        formmain.put("field0003", CrmOrderPayloadSupport.nestedText(raw, "field_HVwgS__c", "label"));
        // field0006  业务员 ← 人员映射 OA人员ID
        formmain.put("field0006", oaUserId);
        // field0007  销售组织 ← 固定值 CAC02
        formmain.put("field0007", CrmOaFormConstants.SALES_ORG);
        // field0008  客户名称 ← customer.name
        formmain.put("field0008", firstNonBlank(
                CrmOrderPayloadSupport.nestedText(raw, "customer", "name"),
                order == null ? null : order.getCustomerName()));
        // field0009  销售币种 ← currency_type.label
        formmain.put("field0009", firstNonBlank(
                CrmOrderPayloadSupport.nestedText(raw, "currency_type", "label"),
                order == null ? null : order.getCurrencyType()));
        // field0010  贸易术语 ← field_TwmQQ__c.label
        formmain.put("field0010", CrmOrderPayloadSupport.nestedText(raw, "field_TwmQQ__c", "label"));
        // field0011  目的地 ← field_NjupX__c.name
        formmain.put("field0011", CrmOrderPayloadSupport.nestedText(raw, "field_NjupX__c", "name"));
        // field0012  运输方式 ← field_65xcf__c.label；空则默认 "-"
        formmain.put("field0012", blankToDefault(
                CrmOrderPayloadSupport.nestedText(raw, "field_65xcf__c", "label"),
                CrmOaFormConstants.EMPTY_PLACEHOLDER));
        // field0013  收款方式 ← field_TZKmt__c.label；空则默认 "-"
        formmain.put("field0013", blankToDefault(
                CrmOrderPayloadSupport.nestedText(raw, "field_TZKmt__c", "label"),
                CrmOaFormConstants.EMPTY_PLACEHOLDER));
        // field0015  订单账期 ← field_9uwgg__c.label
        formmain.put("field0015", CrmOrderPayloadSupport.nestedText(raw, "field_9uwgg__c", "label"));
        // field0017  出运日期 ← field_X7vPP__c.value
        formmain.put("field0017", CrmOrderPayloadSupport.nestedText(raw, "field_X7vPP__c", "value"));
        // field0018  收款日期 ← field_Swpt6__c.value
        formmain.put("field0018", CrmOrderPayloadSupport.nestedText(raw, "field_Swpt6__c", "value"));
        // field0019  内外贸 ← 固定值（暂用默认）
        formmain.put("field0019", CrmOaFormConstants.TRADE_TYPE);
        // field0020  是否集中采购 ← 固定值 "1"
        formmain.put("field0020", CrmOaFormConstants.CENTRAL_PURCHASE);
        // field0025  客户订单号 ← name（订单编号）
        formmain.put("field0025", firstNonBlank(
                CrmOrderPayloadSupport.text(raw, "name"),
                order == null ? null : order.getOrderNo()));
        // field0029  是否需要盖章 ← field_234i0__c.code（1→1，否则→2）
        formmain.put("field0029", mapSealNeed(CrmOrderPayloadSupport.nestedText(raw, "field_234i0__c", "code")));
        // field0031  委托放行单 ← 固定值 "1"
        formmain.put("field0031", CrmOaFormConstants.ENTRUST_RELEASE);
        // field0035  委外方式 ← 固定值 "1"
        formmain.put("field0035", CrmOaFormConstants.OUTSOURCE_MODE);
        // field0222  是否超默认账期 ← null（OA计算公式）
        formmain.put("field0222", null);
        // field0254  客户编码 ← field_zjW8t__c
        formmain.put("field0254", firstNonBlank(
                CrmOrderPayloadSupport.text(raw, "field_zjW8t__c"),
                asPlainText(raw.get("field_zjW8t__c"))));
        // field0327  客户主键 ← 固定值 S1201280054
        formmain.put("field0327", CrmOaFormConstants.CUSTOMER_PK);
        // field0408  原币信用额度 ← 固定值「空值」
        formmain.put("field0408", CrmOaFormConstants.CREDIT_LIMIT);
        // field0461  目的国 ← field_NjupX__c.name（与目的地同源）
        formmain.put("field0461", CrmOrderPayloadSupport.nestedText(raw, "field_NjupX__c", "name"));
        // field0462  出口港 ← field_Z6A3J__c.name
        formmain.put("field0462", CrmOrderPayloadSupport.nestedText(raw, "field_Z6A3J__c", "name"));
        return formmain;
    }

    /**
     * 组装子表单行字段（仅当前明细一行；不传 formson_2818）
     *
     * @param order  订单主表（field0130 取自主订单）
     * @param detail 当前明细
     * @return formson_2819 字段 Map
     */
    public static Map<String, Object> buildFormSon(CrmOrderDO order, CrmOrderDetailDO detail) {
        JsonNode orderRaw = CrmOrderPayloadSupport.asJsonNode(order == null ? null : order.getRawPayload());
        JsonNode detailRaw = CrmOrderPayloadSupport.asJsonNode(detail == null ? null : detail.getRawPayload());
        Map<String, Object> formson = new LinkedHashMap<>();

        // field0074  销售-数量 ← pd_count
        formson.put("field0074", firstNonBlank(
                CrmOrderPayloadSupport.text(detailRaw, "pd_count"),
                detail == null ? null : detail.getPdCount()));
        // field0089  物料编号 ← field_Mb25P__c
        formson.put("field0089", firstNonBlank(
                CrmOrderPayloadSupport.text(detailRaw, "field_Mb25P__c"),
                asPlainText(detailRaw.get("field_Mb25P__c")),
                detail == null ? null : detail.getMaterialCode()));
        // field0091  销售税率 ← null（OA计算公式）
        formson.put("field0091", null);
        // field0092  销售单价 ← actual_price
        formson.put("field0092", firstNonBlank(
                CrmOrderPayloadSupport.text(detailRaw, "actual_price"),
                detail == null ? null : detail.getActualPrice()));
        // field0093  考核单价 ← field_USMmk__c
        formson.put("field0093", firstNonBlank(
                CrmOrderPayloadSupport.text(detailRaw, "field_USMmk__c"),
                asPlainText(detailRaw.get("field_USMmk__c"))));
        // field0129  发货日期 ← 空字符串
        formson.put("field0129", CrmOaFormConstants.SHIP_DATE_EMPTY);
        // field0130  要求到货日 ← 主订单 field_qx94q__c.value
        formson.put("field0130", CrmOrderPayloadSupport.nestedText(orderRaw, "field_qx94q__c", "value"));
        // field0443  零售包装 ← pd_code 以 53 开头→0001，否则→0002
        String pdCode = firstNonBlank(
                CrmOrderPayloadSupport.text(detailRaw, "pd_code"),
                detail == null ? null : detail.getPdCode());
        formson.put("field0443", mapRetailPack(pdCode));
        return formson;
    }

    /**
     * CRM 盖章 code → OA 是否需要盖章（1=是，2=否）
     *
     * @param code CRM field_234i0__c.code
     * @return OA 取值
     */
    public static String mapSealNeed(String code) {
        return CrmOaFormConstants.CRM_SEAL_CODE_YES.equals(code)
                ? CrmOaFormConstants.SEAL_YES
                : CrmOaFormConstants.SEAL_NO;
    }

    /**
     * pd_code 是否零售包装：以 53 开头 → 0001，否则 0002
     *
     * @param pdCode 商品编码，可空
     * @return OA field0443 取值
     */
    public static String mapRetailPack(String pdCode) {
        if (StringUtils.hasText(pdCode) && pdCode.trim().startsWith(CrmOaFormConstants.RETAIL_PD_CODE_PREFIX)) {
            return CrmOaFormConstants.RETAIL_YES;
        }
        return CrmOaFormConstants.RETAIL_NO;
    }

    /**
     * 空值回退默认值
     *
     * @param value        原始值，可空
     * @param defaultValue 默认值
     * @return 非空白原值，否则默认值
     */
    private static String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private static String asPlainText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isObject() || node.isArray()) {
            return null;
        }
        String value = node.asString();
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
