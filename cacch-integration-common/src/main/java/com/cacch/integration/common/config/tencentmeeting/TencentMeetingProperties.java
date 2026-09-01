package com.cacch.integration.common.config.tencentmeeting;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯会议 REST API 配置
 *
 * <p>采用构造器绑定：{@code private final} 字段 + 显式构造器，未配置项回退默认值；
 * 嵌套对象 {@link SmartMinutes} 同样为构造器绑定。</p>
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

    public TencentMeetingProperties(Boolean enabled,
                                    String appId,
                                    String sdkId,
                                    String secretId,
                                    String secretKey,
                                    Integer operatorIdType,
                                    Integer instanceId,
                                    String defaultOperatorId,
                                    SmartMinutes smartMinutes) {
        this.enabled = enabled != null && enabled;
        this.appId = appId;
        this.sdkId = sdkId;
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.operatorIdType = operatorIdType != null ? operatorIdType : 1;
        this.instanceId = instanceId != null ? instanceId : 1;
        this.defaultOperatorId = defaultOperatorId;
        this.smartMinutes = smartMinutes != null
                ? smartMinutes
                : new SmartMinutes(null, null, null, null);
    }

    /**
     * 智能纪要接口参数
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

        public SmartMinutes(Integer textType, Integer llm, Integer minuteType, String lang) {
            this.textType = textType != null ? textType : 2;
            this.llm = llm != null ? llm : 1;
            this.minuteType = minuteType != null ? minuteType : 1;
            this.lang = lang != null ? lang : "default";
        }
    }
}
