package com.cacch.integration.common.config.crm;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * CRM 订单 OA 同步配置
 *
 * <p>采用构造器绑定：单一构造器即被 Spring Boot 自动识别为目标构造器；
 * 所有参数均通过 {@link DefaultValue} 在 yml 缺失对应 key 时回退默认值。</p>
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

    public CrmSyncProperties(
            @DefaultValue("100") Integer batchSize,
            @DefaultValue("3") Integer maxRetry) {
        this.batchSize = batchSize;
        this.maxRetry = maxRetry;
    }
}
