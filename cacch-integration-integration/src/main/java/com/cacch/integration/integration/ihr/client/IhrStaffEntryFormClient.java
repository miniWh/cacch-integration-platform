package com.cacch.integration.integration.ihr.client;

import com.cacch.integration.common.config.ihr.IhrProperties;
import com.cacch.integration.common.constant.ihr.IhrConstants;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffEntryFormCreateRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffEntryFormCreateResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * IHR 新增待入职 HTTP 客户端 — 「新增待入职-简略」
 *
 * <p>接口：POST /openapi/thirdparty/api/staff/v1/entryForms/create
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担（含入参/出参脱敏日志）。
 * 成功判定：{@code code == 0}（与部门清单 v3、职位清单一致）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrStaffEntryFormClient {

    private static final String BIZ = IhrConstants.LOG_BIZ;
    private static final String ACTION_CREATE_ENTRY_FORM = "新增待入职-简略";

    private final RestTemplateHttpClient httpClient;
    private final IhrProperties ihrProperties;

    /**
     * 调用 IHR 新增待入职接口
     *
     * <p>请求体中 {@code idCardNo}、{@code mobile} 为敏感字段，
     * 脱敏由 {@link RestTemplateHttpClient} 内部的 {@code ThirdPartyHttpLogSupport} 自动处理。</p>
     *
     * @param accessToken IHR access_token（Bearer header 值）
     * @param requestBody 待入职请求体（不可为 null）
     * @return iHR 原始响应；业务 code != 0 或 HTTP 非 2xx 由 Service 层处理
     */
    public IhrStaffEntryFormCreateResponse createEntryForm(String accessToken,
                                                           IhrStaffEntryFormCreateRequest requestBody) {
        String url = ihrProperties.getBaseUrl() + IhrConstants.STAFF_ENTRY_FORM_CREATE_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        IhrStaffEntryFormCreateResponse body = httpClient.doPostOrPut(
                HttpMethod.POST, url, requestBody, headers,
                BIZ, ACTION_CREATE_ENTRY_FORM,
                IhrStaffEntryFormCreateResponse.class);

        if (body.isSuccess()) {
            log.info("【{}】{}成功, code={}, entryId={}, added={}",
                    BIZ, ACTION_CREATE_ENTRY_FORM, body.getCode(), body.getEntryId(), body.getData());
        } else {
            log.info("【{}】{}终止, code={}, message={}", BIZ, ACTION_CREATE_ENTRY_FORM, body.getCode(), body.getMessage());
        }
        return body;
    }
}
