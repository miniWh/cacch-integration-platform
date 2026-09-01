package com.cacch.integration.common.config.crm;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * CRM 订单采集配置
 *
 * <p>采用构造器绑定：单一构造器即被 Spring Boot 自动识别为目标构造器；
 * 所有参数均通过 {@link DefaultValue} 在 yml 缺失对应 key 时回退默认值。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "crm.collect")
public class CrmCollectProperties {

    /**
     * 单轮最多处理订单笔数（含新拉取与补拉占用），默认 100
     */
    private final int batchSize;

    /**
     * 明细查询每页条数，默认 100
     */
    private final int detailPageSize;

    /**
     * 补拉明细最多处理笔数；{@code <=0} 时与 {@link #batchSize} 相同
     */
    private final int detailRetryBatchSize;

    public CrmCollectProperties(
            @DefaultValue("100") Integer batchSize,
            @DefaultValue("100") Integer detailPageSize,
            @DefaultValue("100") Integer detailRetryBatchSize) {
        this.batchSize = batchSize;
        this.detailPageSize = detailPageSize;
        this.detailRetryBatchSize = detailRetryBatchSize;
    }
}
