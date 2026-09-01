package com.cacch.integration.common.config.crm;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CRM 订单采集配置
 *
 * <p>采用构造器绑定：{@code private final} 字段 + 显式构造器，未配置项回退默认值。</p>
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

    public CrmCollectProperties(Integer batchSize, Integer detailPageSize, Integer detailRetryBatchSize) {
        this.batchSize = batchSize != null ? batchSize : 100;
        this.detailPageSize = detailPageSize != null ? detailPageSize : 100;
        this.detailRetryBatchSize = detailRetryBatchSize != null ? detailRetryBatchSize : 100;
    }
}
