package com.cacch.integration.common.config.crm;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CRM 订单 OA 同步配置
 *
 * <p>采用构造器绑定：{@code private final} 字段 + 显式构造器，未配置项回退默认值。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "crm.sync")
public class CrmSyncProperties {

    /**
     * 单轮最多同步明细条数（即最多发起 OA 表单数），默认 100
     */
    private final int batchSize;

    /**
     * OA 同步最大重试次数；达到后标记 FAILED 并告警，默认 3
     */
    private final int maxRetry;

    public CrmSyncProperties(Integer batchSize, Integer maxRetry) {
        this.batchSize = batchSize != null ? batchSize : 100;
        this.maxRetry = maxRetry != null ? maxRetry : 3;
    }
}
