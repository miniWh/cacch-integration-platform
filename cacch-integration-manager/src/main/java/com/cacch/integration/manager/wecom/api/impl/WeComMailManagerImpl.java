package com.cacch.integration.manager.wecom.api.impl;

import com.cacch.integration.common.config.wecom.WeComAppConfig;
import com.cacch.integration.common.config.wecom.WeComProperties;
import com.cacch.integration.common.constant.wecom.WeComMailLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListRequest;
import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListResponse;
import com.cacch.integration.manager.wecom.api.IWeComMailManager;
import com.cacch.integration.manager.wecom.api.IWeComTokenManager;
import com.cacch.integration.service.wecom.api.IWeComMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 企微邮件编排实现
 *
 * <p>负责解析自建应用配置并获取 access_token，再委托邮件服务完成接口调用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComMailManagerImpl implements IWeComMailManager {

    private final IWeComMailService weComMailService;
    private final IWeComTokenManager weComTokenManager;
    private final WeComProperties weComProperties;

    @Override
    public WeComMailListResponse getMailList(WeComMailListRequest request) {
        WeComAppConfig appConfig = weComProperties.findSelfBuiltApp().orElse(null);
        if (appConfig == null) {
            log.info("【{}】获取收件箱邮件列表终止, reason=未配置企微自建应用",
                    WeComMailLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "未配置企微自建应用（wecom.apps app-key=self-built）");
        }
        String accessToken = weComTokenManager.getAccessToken(appConfig.getCorpid(), appConfig.getAppKey());
        return weComMailService.getMailList(accessToken, request);
    }
}
