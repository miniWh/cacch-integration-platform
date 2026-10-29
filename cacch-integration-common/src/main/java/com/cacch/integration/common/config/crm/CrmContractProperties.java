package com.cacch.integration.common.config.crm;

import com.cacch.integration.common.constant.crm.CrmContractConstants;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CRM 修改销售合同（contractEdit）配置属性（REQ-OA-003）
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "crm.contract")
public class CrmContractProperties {

    /**
     * 附件字段 key：合同自定义字段标识（联调可调整）
     */
    private final String attachmentFieldKey;

    public CrmContractProperties(String attachmentFieldKey) {
        this.attachmentFieldKey = attachmentFieldKey != null && !attachmentFieldKey.isBlank()
                ? attachmentFieldKey.trim()
                : CrmContractConstants.DEFAULT_ATTACHMENT_FIELD_KEY;
    }
}
