package com.cacch.integration.config.oa;

import com.cacch.integration.common.config.oa.OaContractAttachmentSyncProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OA 货物购销合同附件回传 CRM 配置注册
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(OaContractAttachmentSyncProperties.class)
public class OaContractAttachmentConfiguration {
}
