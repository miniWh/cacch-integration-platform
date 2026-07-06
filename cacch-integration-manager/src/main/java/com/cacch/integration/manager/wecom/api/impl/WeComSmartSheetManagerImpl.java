package com.cacch.integration.manager.wecom.api.impl;

import com.cacch.integration.common.config.wecom.WeComAppConfig;
import com.cacch.integration.common.config.wecom.WeComProperties;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordWriteItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateRecordsResponse;
import com.cacch.integration.manager.wecom.api.IWeComSmartSheetManager;
import com.cacch.integration.manager.wecom.api.IWeComTokenManager;
import com.cacch.integration.service.wecom.api.IWeComSmartSheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * 企业微信智能表格编排实现 — 使用自建应用配置获取 access_token 后调用 Service
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComSmartSheetManagerImpl implements IWeComSmartSheetManager {

    private final IWeComSmartSheetService weComSmartSheetService;
    private final IWeComTokenManager weComTokenManager;
    private final WeComProperties weComProperties;

    @Override
    public WeComGetSheetResponse getSheets(String docId, String sheetId, Boolean needAllTypeSheet) {
        return execute("查询子表", () ->
                weComSmartSheetService.getSheets(resolveAccessToken(), docId, sheetId, needAllTypeSheet));
    }

    @Override
    public WeComGetFieldsResponse getFields(String docId, String sheetId, Integer offset, Integer limit) {
        return execute("查询字段", () ->
                weComSmartSheetService.getFields(resolveAccessToken(), docId, sheetId, offset, limit));
    }

    @Override
    public WeComGetRecordsResponse getRecords(String docId, String sheetId, Integer offset, Integer limit) {
        return execute("查询记录", () ->
                weComSmartSheetService.getRecords(resolveAccessToken(), docId, sheetId, offset, limit));
    }

    @Override
    public WeComAddRecordsResponse addRecords(String docId, String sheetId, List<WeComRecordWriteItem> records) {
        return execute("添加记录", () ->
                weComSmartSheetService.addRecords(resolveAccessToken(), docId, sheetId, records));
    }

    @Override
    public WeComUpdateRecordsResponse updateRecords(String docId, String sheetId, List<WeComRecordWriteItem> records) {
        return execute("更新记录", () ->
                weComSmartSheetService.updateRecords(resolveAccessToken(), docId, sheetId, records));
    }

    private String resolveAccessToken() {
        WeComAppConfig appConfig = weComProperties.findSelfBuiltApp()
                .orElseThrow(() -> new BizException(ResultCode.PARAM_INVALID, "未配置企微自建应用（wecom.apps）"));
        log.debug("【WeComSmartSheet】使用自建应用鉴权, corpid={}, appKey={}",
                appConfig.getCorpid(), appConfig.getAppKey());
        return weComTokenManager.getAccessToken(appConfig.getCorpid(), appConfig.getAppKey());
    }

    private <T> T execute(String action, SmartSheetCall<T> call) {
        try {
            return call.run();
        } catch (BizException e) {
            log.error("【WeComSmartSheet】编排层{}失败, errCode={}, errMsg={}", action, e.getCode(), e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.error("【WeComSmartSheet】编排层{} HTTP 异常", action, e);
            throw new BizException(ResultCode.INTEGRATION_TIMEOUT, "企业微信智能表格" + action + "超时", e);
        } catch (Exception e) {
            log.error("【WeComSmartSheet】编排层{}发生未知异常", action, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "企业微信智能表格" + action + "失败", e);
        }
    }

    @FunctionalInterface
    private interface SmartSheetCall<T> {
        T run();
    }
}
