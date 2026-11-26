package com.cacch.integration.common.config.heart;

import com.cacch.integration.common.constant.heart.HeartMailConstants;
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
     * 邮箱拉取参数
     */
    private Mail mail = new Mail();

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

    /**
     * 邮箱拉取参数（IMAP）
     */
    @Data
    public static class Mail {

        /**
         * IMAP 服务器地址
         */
        private String host = "imap.exmail.qq.com";

        /**
         * IMAP 端口
         */
        private Integer port = 993;

        /**
         * 是否启用 SSL
         */
        private Boolean ssl = Boolean.TRUE;

        /**
         * 邮箱账号
         */
        private String username;

        /**
         * 客户端专用密码/授权码
         */
        private String password;

        /**
         * 邮件文件夹
         */
        private String folder = HeartMailConstants.DEFAULT_FOLDER;

        /**
         * 单轮最多处理邮件数
         */
        private Integer maxPerRound = HeartMailConstants.DEFAULT_MAX_PER_ROUND;
    }
}
