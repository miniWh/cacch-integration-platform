package com.cacch.integration.common.constant.crm;

/**
 * CRM 修改销售合同（contractEdit）专有常量（REQ-OA-003）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmContractConstants {

    private CrmContractConstants() {
    }

    /**
     * 附件字段 key 默认值（合同自定义字段，对应配置 crm.contract.attachment-field-key，联调可调整）
     */
    public static final String DEFAULT_ATTACHMENT_FIELD_KEY = "field_quRkU__c";

    /**
     * 附件对象属性：文件访问地址（纯 URL，无反引号，不传 fileSize）
     */
    public static final String ATTACHMENT_PROP_PATH = "path";

    /**
     * 附件对象属性：文件名
     */
    public static final String ATTACHMENT_PROP_NAME = "name";

    /**
     * fields 内合同数据 ID 字段名
     */
    public static final String CONTRACT_FIELD_ID = "id";
}
