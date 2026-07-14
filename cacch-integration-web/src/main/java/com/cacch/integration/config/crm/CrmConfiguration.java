package com.cacch.integration.config.crm;

import com.cacch.integration.common.config.crm.CrmCollectProperties;
import com.cacch.integration.common.config.crm.CrmProperties;
import com.cacch.integration.common.config.crm.CrmSyncProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 勤策 CRM 配置注册
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties({CrmProperties.class, CrmCollectProperties.class, CrmSyncProperties.class})
public class CrmConfiguration {
}
