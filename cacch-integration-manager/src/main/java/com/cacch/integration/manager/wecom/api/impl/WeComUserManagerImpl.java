package com.cacch.integration.manager.wecom.api.impl;

import com.cacch.integration.common.config.wecom.WeComAppConfig;
import com.cacch.integration.common.config.wecom.WeComProperties;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.dto.user.WeComGetUserResponse;
import com.cacch.integration.manager.wecom.api.IWeComTokenManager;
import com.cacch.integration.manager.wecom.api.IWeComUserManager;
import com.cacch.integration.service.wecom.api.IWeComUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

/**
 * 企微通讯录编排实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComUserManagerImpl implements IWeComUserManager {

    private final IWeComUserService weComUserService;
    private final IWeComTokenManager weComTokenManager;
    private final WeComProperties weComProperties;

    @Override
    public String getUserName(String userid) {
        if (!StringUtils.hasText(userid)) {
            log.info("【WeComUser】查询成员姓名终止, reason=userid为空");
            return null;
        }
        try {
            WeComGetUserResponse response = weComUserService.getUser(resolveAccessToken(), userid.trim());
            String name = response.getName();
            if (!StringUtils.hasText(name)) {
                log.info("【WeComUser】查询成员姓名终止, userid={}, reason=姓名为空", userid);
                return null;
            }
            return name.trim();
        } catch (BizException e) {
            log.info("【WeComUser】查询成员姓名终止, userid={}, reason={}", userid, e.getMessage());
            return null;
        } catch (RestClientException e) {
            log.info("【WeComUser】查询成员姓名终止, userid={}, reason={}", userid, e.getMessage());
            log.error("【WeComUser】查询成员姓名 HTTP 异常, userid={}", userid, e);
            return null;
        } catch (Exception e) {
            log.info("【WeComUser】查询成员姓名终止, userid={}, reason={}", userid, e.getMessage());
            log.error("【WeComUser】查询成员姓名发生未知异常, userid={}", userid, e);
            return null;
        }
    }

    private String resolveAccessToken() {
        WeComAppConfig appConfig = weComProperties.findAddressBookApp()
                .orElseThrow(() -> new BizException(ResultCode.PARAM_INVALID,
                        "未配置企微通讯录应用（wecom.apps app-key=address-book）"));
        return weComTokenManager.getAccessToken(appConfig.getCorpid(), appConfig.getAppKey());
    }
}
