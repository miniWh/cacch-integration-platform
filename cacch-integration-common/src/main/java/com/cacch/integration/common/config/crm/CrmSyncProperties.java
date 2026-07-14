package com.cacch.integration.common.config.crm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CRM 订单 OA 同步配置
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "crm.sync")
public class CrmSyncProperties {

    /**
     * 单轮最多同步明细条数（即最多发起 OA 表单数），默认 100
     */
    private int batchSize = 100;

    /**
     * OA 同步最大重试次数；达到后标记 FAILED 并告警，默认 3
     */
    private int maxRetry = 3;
}
