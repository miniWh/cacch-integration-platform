package com.cacch.integration.common.config.tencentmeeting;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯会议 REST API 配置
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "tencent-meeting")
public class TencentMeetingProperties {

    private boolean enabled = false;

    private String appId;

    private String sdkId;

    private String secretId;

    private String secretKey;

    /**
     * 操作者 ID 类型：1=userid，2=openid
     */
    private int operatorIdType = 1;

    /**
     * 终端设备类型，查询会议接口必填
     */
    private int instanceId = 1;

    /**
     * 无会议主持人时的兜底 operator_id（企微 userid，调用腾讯会议 API 前会映射）
     */
    private String defaultOperatorId;

    private final SmartMinutes smartMinutes = new SmartMinutes();

    /**
     * 智能纪要接口参数
     */
    @Getter
    @Setter
    public static class SmartMinutes {

        /**
         * 返回文本类型：1=纯文本，2=markdown
         */
        private int textType = 2;

        /**
         * 纪要模型：1=混元，2=DeepSeek，3=元宝纪要
         */
        private int llm = 1;

        /**
         * 会议摘要返回类别（llm=1 时生效）：1=按章节，2=按主题，3=按发言人
         */
        private int minuteType = 1;

        /**
         * 翻译类型：default/zh/en/ja
         */
        private String lang = "default";
    }
}
