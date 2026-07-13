package com.cacch.integration.dto.oa.request;

import lombok.Data;

/**
 * 获取 / 清除 OA Token 请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaTokenRequest {

    /**
     * 绑定 OA 登录名；为空则用配置 {@code oa.default-login-name}
     */
    private String loginName;
}
