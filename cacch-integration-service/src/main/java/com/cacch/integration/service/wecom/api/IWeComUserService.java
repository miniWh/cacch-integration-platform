package com.cacch.integration.service.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.user.WeComGetUserResponse;

/**
 * 企业微信通讯录服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComUserService {

    /**
     * 按 userid 读取成员详情
     *
     * @param accessToken 企微 access_token，不可为空
     * @param userid      成员 UserID，不可为空
     * @return 成员详情（含 name）
     */
    WeComGetUserResponse getUser(String accessToken, String userid);
}
