package com.cacch.integration.service.ihr.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.IhrStaffEntryFormClient;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffEntryFormCreateRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffEntryFormCreateResponse;
import com.cacch.integration.service.ihr.api.IIhrStaffEntryFormService;
import com.cacch.integration.service.ihr.api.IIhrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

/**
 * IHR 新增待入职 Service 实现
 *
 * <p>HTTP 401/403 时强制刷新 token 后重试一次（与 {@code IhrPositionServiceImpl.fetchAll} 模式一致）。
 * 本接口无数据库读写，全程无事务。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IhrStaffEntryFormServiceImpl implements IIhrStaffEntryFormService {

    private static final String BIZ = "IHR待入职Service";

    private final IIhrTokenService ihrTokenService;
    private final IhrStaffEntryFormClient ihrStaffEntryFormClient;

    @Override
    public IhrStaffEntryFormCreateResponse createEntryForm(IhrStaffEntryFormCreateRequest requestBody) {
        String accessToken = ihrTokenService.getAccessToken();
        IhrStaffEntryFormCreateResponse response;
        try {
            response = ihrStaffEntryFormClient.createEntryForm(accessToken, requestBody);
        } catch (HttpStatusCodeException e) {
            // 401/403：强制刷新 token 后重试一次
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED
                    || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.info("【{}】HTTP {}，强制刷新 token 后重试", BIZ, e.getStatusCode().value());
                accessToken = ihrTokenService.forceRefresh();
                response = ihrStaffEntryFormClient.createEntryForm(accessToken, requestBody);
            } else {
                log.info("【{}】HTTP 调用终止, status={}", BIZ, e.getStatusCode().value());
                throw e;
            }
        } catch (RestClientException e) {
            log.info("【{}】HTTP 调用终止, reason={}", BIZ, e.getMessage());
            throw e;
        }

        if (response == null || !response.isSuccess()) {
            log.info("【{}】接口终止, code={}, message={}",
                    BIZ, response == null ? "null" : response.getCode(),
                    response == null ? "空响应" : response.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 新增待入职失败: code=" + (response == null ? "null" : response.getCode())
                            + ", message=" + (response == null ? "空响应" : response.getMessage()));
        }
        return response;
    }
}
