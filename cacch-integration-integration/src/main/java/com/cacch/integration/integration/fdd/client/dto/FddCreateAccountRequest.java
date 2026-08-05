package com.cacch.integration.integration.fdd.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * 法大大创建用户请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FddCreateAccountRequest {

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 手机号区号，默认 +86
     */
    private String areaCode;

    /**
     * 用户手机号
     */
    private String mobile;

    /**
     * 第三方业务系统用户唯一标识（本场景使用身份证号）
     */
    private String tpAccountId;
}
