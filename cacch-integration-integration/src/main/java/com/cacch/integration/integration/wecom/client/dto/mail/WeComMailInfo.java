package com.cacch.integration.integration.wecom.client.dto.mail;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 企业微信邮件信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeComMailInfo {

    /**
     * 邮件 ID
     */
    @JsonProperty("mail_id")
    @JsonAlias("mailId")
    private String mailId;
}
