package com.cacch.integration.common.config.crm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CRM 订单采集配置
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "crm.collect")
public class CrmCollectProperties {

    /**
     * 单轮最多处理订单笔数（含新拉取与补拉占用），默认 100
     */
    private int batchSize = 100;

    /**
     * 明细查询每页条数，默认 100
     */
    private int detailPageSize = 100;

    /**
     * 补拉明细最多处理笔数；{@code <=0} 时与 {@link #batchSize} 相同
     */
    private int detailRetryBatchSize = 100;
}
