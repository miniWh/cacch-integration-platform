package com.cacch.integration.common.constant.heart;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Heart 销售合同 contractAdd 报文常量
 *
 * <p>集中承载与 Heart 业务无关的勤策固定取值（自定义字段值、费用类型、供应商、从对象 key），
 * 避免在组装代码中散落魔法值；可变字段 key 也在此声明，便于联调调整。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartContractConstants {

    private HeartContractConstants() {
    }

    /**
     * 合同/明细币种
     */
    public static final String CURRENCY_CNY = "CNY";

    /**
     * 汇率
     */
    public static final String EXCHANGE_RATE_ONE = "1";

    /**
     * 千升与 L 的换算系数
     */
    public static final BigDecimal LITER_PER_KILOLITER = new BigDecimal("1000");

    /**
     * 明细单位名称（暂统一按千升）
     */
    public static final String UNIT_NAME_KILOLITER = "千升";

    /**
     * 报关口岸兜底对象 ID（上海；启运港对象 ID 为空时使用）
     */
    public static final String CUSTOMS_PORT_FALLBACK_ID = "5801559508238571560";

    /**
     * 客户余额占位值（ERP 接口未接入前），按字符串类型传输
     */
    public static final String CUSTOMER_BALANCE_PLACEHOLDER = "0";

    /**
     * 毛利率缺省值，按数值类型传输
     */
    public static final BigDecimal GROSS_MARGIN_PLACEHOLDER = BigDecimal.ZERO;

    /**
     * 出运计划行固定比例
     */
    public static final BigDecimal SHIPMENT_RATE = new BigDecimal("0.45");

    /**
     * 出运计划从对象 key
     */
    public static final String OBJECT_SHIPMENT_PLAN = "object_t3TPJ__c";

    /**
     * 费用从对象 key
     */
    public static final String OBJECT_FEE = "object_CmMeu__c";

    /**
     * 报关明细从对象 key
     */
    public static final String OBJECT_CUSTOMS = "object_liHgv__c";

    /**
     * 包干费费用类型 ID
     */
    public static final String FEE_TYPE_PACKAGE = "8335140024167817834";

    /**
     * 信保费费用类型 ID
     */
    public static final String FEE_TYPE_INSURANCE = "5756541997336160304";

    /**
     * 订舱费费用类型 ID
     */
    public static final String FEE_TYPE_BOOKING = "4686499693079512310";

    /**
     * 包干费供应商 ID
     */
    public static final String SUPPLIER_PACKAGE = "5916160457912923257";

    /**
     * 订舱费供应商 ID
     */
    public static final String SUPPLIER_BOOKING = "6263697857743890577";

    /**
     * 合同附件字段 key
     */
    public static final String FIELD_ATTACHMENT = "field_8s0nt__c";

    /**
     * 客户 PO 字段 key
     */
    public static final String FIELD_CLIENT_PO = "field_89iqq__c";

    /**
     * 客户余额字段 key
     */
    public static final String FIELD_CUSTOMER_BALANCE = "field_InoJC__c";

    /**
     * 目的港字段 key
     */
    public static final String FIELD_DISCHARGE_PORT = "field_XGQ4B__c";

    /**
     * 目的地字段 key
     */
    public static final String FIELD_DESTINATION = "field_RHVAG__c";

    /**
     * 报关口岸字段 key
     */
    public static final String FIELD_CUSTOMS_PORT = "field_W2CUi__c";

    /**
     * 毛利率字段 key（合同级）
     */
    public static final String FIELD_GROSS_MARGIN_A = "field_4TfYT__c";

    /**
     * 毛利率字段 key（合同级）
     */
    public static final String FIELD_GROSS_MARGIN_B = "field_M1ODf__c";

    /**
     * CAC PO 字段 key
     */
    public static final String FIELD_CAC_PO = "field_GOYSU__c";

    /**
     * 合同级固定自定义字段
     */
    public static final Map<String, Object> FIXED_EXT_FIELDS;

    /**
     * 明细级固定自定义字段
     */
    public static final Map<String, Object> FIXED_DETAIL_EXT_FIELDS;

    static {
        Map<String, Object> contractFields = new LinkedHashMap<>();
        contractFields.put("field_Kd0sT__c", "0");
        contractFields.put("field_OrderType__c", "30_Cxx_01");
        contractFields.put("field_IJ1vy__c", "6882794459373827339");
        contractFields.put("field_iCdZL__c", "1");
        contractFields.put("field_Of12R__c", "w3skwztN");
        contractFields.put("field_DiTIc__c", "6288291750959183118");
        contractFields.put("field_u37QF__c", "01");
        contractFields.put("field_2oUdI__c", "FOB");
        contractFields.put("field_L22xK__c", "08");
        contractFields.put("field_6RG5F__c", "q5qcsq62");
        contractFields.put("field_9vwB1__c", "2");
        contractFields.put("field_fUPcJ__c", "2");
        FIXED_EXT_FIELDS = Collections.unmodifiableMap(contractFields);

        Map<String, Object> detailFields = new LinkedHashMap<>();
        detailFields.put("field_5xbnz__c", "1");
        detailFields.put("field_Orije__c", "b");
        FIXED_DETAIL_EXT_FIELDS = Collections.unmodifiableMap(detailFields);
    }
}
