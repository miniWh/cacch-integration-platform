package com.cacch.integration.integration.wecom.client.dto.mail;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 企业微信获取收件箱邮件列表响应
 *
 * <p>企微响应信封为顶层 errcode/errmsg，非 response_data 结构，
 * 故复用统一的 {@link WeComBaseResponse}（信封字段只声明一次），
 * 本类仅声明邮件列表业务字段。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComMailListResponse extends WeComBaseResponse {

    /**
     * 下一次调用时传入的翻页游标
     */
    @JsonProperty("next_cursor")
    private String nextCursor;

    /**
     * 是否还有更多数据：0-没有，1-有
     */
    @JsonProperty("has_more")
    private Integer hasMore;

    /**
     * 邮件列表
     */
    @JsonProperty("mail_list")
    private List<WeComMailInfo> mailList;
}
