package com.cacch.integration.common.config.heart;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Heart 订单自动化配置属性
 *
 * <p>配置 POJO 定义在 common 模块，由 web 模块通过 {@code @EnableConfigurationProperties} 注册为 Bean。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@ConfigurationProperties(prefix = "heart")
public class HeartProperties {

    /**
     * 建单相关 CRM 固定参数
     */
    private Crm crm = new Crm();

    /**
     * Heart 建单 CRM 固定参数
     */
    @Data
    public static class Crm {

        /**
         * 合同客户编码
         */
        private String customerCode = "S1212000018";

        /**
         * 合同负责人 ID
         */
        private String ownerId = "6291185993388435139";

        /**
         * 工作流触发员工 ID
         */
        private String triggerEmployee = "6288291750959183118";

        /**
         * 建单后是否触发工作流
         */
        private Boolean isTrigger = Boolean.TRUE;
    }
}
