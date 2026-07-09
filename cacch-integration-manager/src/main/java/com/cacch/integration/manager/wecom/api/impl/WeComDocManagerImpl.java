package com.cacch.integration.manager.wecom.api.impl;

import com.cacch.integration.common.config.wecom.WeComAppConfig;
import com.cacch.integration.common.config.wecom.WeComProperties;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocResponse;
import com.cacch.integration.manager.wecom.api.IWeComDocManager;
import com.cacch.integration.manager.wecom.api.IWeComTokenManager;
import com.cacch.integration.service.wecom.api.IWeComDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * 企微文档编排实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComDocManagerImpl implements IWeComDocManager {

    private final IWeComDocService weComDocService;
    private final IWeComTokenManager weComTokenManager;
    private final WeComProperties weComProperties;

    @Override
    public WeComCreateDocResponse createSmartSheetDoc(String docName, List<String> adminUsers) {
        try {
            return weComDocService.createSmartSheetDoc(resolveAccessToken(), docName, adminUsers);
        } catch (BizException e) {
            log.info("【WeComDoc】编排层新建智能表格终止, docName={}, reason={}", docName, e.getMessage());
            log.error("【WeComDoc】编排层新建智能表格失败, errCode={}, errMsg={}", e.getCode(), e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.info("【WeComDoc】编排层新建智能表格终止, docName={}, reason={}", docName, e.getMessage());
            log.error("【WeComDoc】编排层新建智能表格 HTTP 异常", e);
            throw new BizException(ResultCode.INTEGRATION_TIMEOUT, "企业微信新建智能表格超时", e);
        } catch (Exception e) {
            log.info("【WeComDoc】编排层新建智能表格终止, docName={}, reason={}", docName, e.getMessage());
            log.error("【WeComDoc】编排层新建智能表格发生未知异常", e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "企业微信新建智能表格失败", e);
        }
    }

    private String resolveAccessToken() {
        WeComAppConfig appConfig = weComProperties.findSelfBuiltApp()
                .orElseThrow(() -> new BizException(ResultCode.PARAM_INVALID, "未配置企微自建应用（wecom.apps）"));
        return weComTokenManager.getAccessToken(appConfig.getCorpid(), appConfig.getAppKey());
    }
}
