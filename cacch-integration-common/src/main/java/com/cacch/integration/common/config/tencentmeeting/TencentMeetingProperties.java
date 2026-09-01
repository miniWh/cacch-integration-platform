package com.cacch.integration.common.config.tencentmeeting;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 腾讯会议 REST API 配置
 *
 * <p>采用构造器绑定：单一构造器即被 Spring Boot 自动识别为目标构造器；所有参数均通过
 * {@link DefaultValue} 在 yml 缺失对应 key 时回退默认值；嵌套对象 {@link SmartMinutes}
 * 同样为单一构造器自动绑定。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "tencent-meeting")
public class TencentMeetingProperties {

    private final boolean enabled;

    private final String appId;

    private final String sdkId;

    private final String secretId;

    private final String secretKey;

    /**
     * 操作者 ID 类型：1=userid，2=openid
     */
    private final int operatorIdType;

    /**
     * 终端设备类型，查询会议接口必填
     */
    private final int instanceId;

    /**
     * 无会议主持人时的兜底 operator_id（企微 userid，调用腾讯会议 API 前会映射）
     */
    private final String defaultOperatorId;

    /**
     * 智能纪要接口参数
     */
    private final SmartMinutes smartMinutes;

    public TencentMeetingProperties(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("") String appId,
            @DefaultValue("") String sdkId,
            @DefaultValue("") String secretId,
            @DefaultValue("") String secretKey,
            @DefaultValue("1") Integer operatorIdType,
            @DefaultValue("1") Integer instanceId,
            @DefaultValue("") String defaultOperatorId,
            @DefaultValue SmartMinutes smartMinutes) {
        this.enabled = enabled;
        this.appId = appId;
        this.sdkId = sdkId;
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.operatorIdType = operatorIdType;
        this.instanceId = instanceId;
        this.defaultOperatorId = defaultOperatorId;
        // 显式 null-兜底：当 smart-minutes 整段缺失时构造默认实例
        this.smartMinutes = smartMinutes != null
                ? smartMinutes
                : new SmartMinutes(2, 1, 1, "default");
    }

    /**
     * 智能纪要接口参数 — 嵌套值对象，单一构造器自动绑定
     */
    @Getter
    public static class SmartMinutes {

        /**
         * 返回文本类型：1=纯文本，2=markdown
         */
        private final int textType;

        /**
         * 纪要模型：1=混元，2=DeepSeek，3=元宝纪要
         */
        private final int llm;

        /**
         * 会议摘要返回类别（llm=1 时生效）：1=按章节，2=按主题，3=按发言人
         */
        private final int minuteType;

        /**
         * 翻译类型：default/zh/en/ja
         */
        private final String lang;

        public SmartMinutes(
                @DefaultValue("2") Integer textType,
                @DefaultValue("1") Integer llm,
                @DefaultValue("1") Integer minuteType,
                @DefaultValue("default") String lang) {
            this.textType = textType;
            this.llm = llm;
            this.minuteType = minuteType;
            this.lang = lang;
        }
    }
}
