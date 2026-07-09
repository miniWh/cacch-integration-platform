package com.cacch.integration.service.wecom.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.WeComUserClient;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.cacch.integration.integration.wecom.client.dto.user.WeComGetUserResponse;
import com.cacch.integration.service.wecom.api.IWeComUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 企微通讯录服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComUserServiceImpl implements IWeComUserService {

    private final WeComUserClient weComUserClient;

    @Override
    public WeComGetUserResponse getUser(String accessToken, String userid) {
        if (!StringUtils.hasText(userid)) {
            throw new BizException(ResultCode.PARAM_INVALID, "userid 不能为空");
        }
        WeComGetUserResponse response = weComUserClient.getUser(accessToken, userid.trim());
        assertWeComSuccess(response, "读取成员详情");
        return response;
    }

    private void assertWeComSuccess(WeComBaseResponse response, String action) {
        if (!response.isSuccess()) {
            log.info("【WeComUser】{}终止, errcode={}, reason={}",
                    action, response.getErrCode(), response.getErrMsg());
            log.error("【WeComUser】{}失败, errcode={}, errmsg={}",
                    action, response.getErrCode(), response.getErrMsg());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    String.format("企业微信%s失败, errcode=%d, errmsg=%s",
                            action, response.getErrCode(), response.getErrMsg()));
        }
    }
}
