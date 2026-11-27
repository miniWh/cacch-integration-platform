package com.cacch.integration.common.config.heart;

import com.cacch.integration.common.constant.heart.HeartAiConstants;
import com.cacch.integration.common.constant.heart.HeartMailConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

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
     * PO 智能解析参数
     */
    private Ai ai = new Ai();

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

    /**
     * PO 智能解析参数（OpenAI 兼容接口）
     */
    @Data
    public static class Ai {

        /**
         * 是否启用 AI 解析（关闭时直接走 PDF 文本兜底）
         */
        private Boolean enabled = Boolean.TRUE;

        /**
         * 模型服务地址
         */
        private String baseUrl;

        /**
         * API 密钥（环境变量注入，禁止硬编码与打印）
         */
        private String apiKey;

        /**
         * 模型名称
         */
        private String model;

        /**
         * 提示词版本
         */
        private String promptVersion = HeartAiConstants.PROMPT_VERSION;

        /**
         * 采样温度，固定 0 保证稳定输出
         */
        private Double temperature = 0D;

        /**
         * 单次调用超时毫秒数
         */
        private Integer timeoutMs = HeartAiConstants.DEFAULT_TIMEOUT_MS;

        /**
         * 失败重试次数
         */
        private Integer retryTimes = HeartAiConstants.DEFAULT_RETRY_TIMES;

        /**
         * 自动通过置信度阈值
         */
        private BigDecimal confidenceThreshold = HeartAiConstants.CONFIDENCE_THRESHOLD;

        /**
         * 降级置信度阈值（低于该值转文本兜底）
         */
        private BigDecimal fallbackThreshold = HeartAiConstants.FALLBACK_THRESHOLD;

        /**
         * 送入模型的最大文本字符数
         */
        private Integer maxTextChars = HeartAiConstants.DEFAULT_MAX_TEXT_CHARS;
    }
}
