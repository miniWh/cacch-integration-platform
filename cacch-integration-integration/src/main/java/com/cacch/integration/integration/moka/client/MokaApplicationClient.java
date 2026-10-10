package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Moka 入离职 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #resignApplication(Integer, MokaApplicationResignRequest)} — 标记候选人已离职
 *         （PUT /api-platform/v1/applications/{applicationId}/resign）</li>
 * </ul>
 *
 * <p>HTTP 通用调用（Basic Auth、URI 构造、出入参日志、异常兜底）统一由
 * {@link MokaHttpClient} 承担，本类仅保留 URL 拼接、方法级前置校验、成功 INFO。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaApplicationClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_RESIGN_APPLICATION = "标记候选人已离职";

    private final MokaHttpClient mokaHttpClient;
    private final MokaProperties mokaProperties;

    /**
     * 标记候选人已离职 —— 调用 Moka {@code PUT /api-platform/v1/applications/{applicationId}/resign}
     *
     * <p>将指定申请归档为已离职。请求体中除 {@code detail}（离职原因）必填外，
     * 其余字段均可选；{@code isResignWriteJob=1} 时 {@code resignJobName} 必填
     * （该条件校验由上层负责，Client 不重复校验）。</p>
     *
     * <p>响应成功判定使用 {@code code} 字段（Moka 官方已废弃 success 字段，
     * 严禁用于业务判断）；成功码兼容 {"0", "200", "success"}。</p>
     *
     * @param applicationId 候选人申请 id（必填，大于 0），来自 Moka 申请查询接口
     * @param request       离职请求体（必填，detail 必填）
     * @return Moka 离职操作响应（code + errorMessage）
     * @throws RestClientException applicationId 非法、请求体为空时抛出
     */
    public MokaApplicationResignResponse resignApplication(Integer applicationId, MokaApplicationResignRequest request) {
        // —— 方法级前置校验（MokaHttpClient 只做 API Key 守卫）——
        if (applicationId == null || applicationId <= 0) {
            log.info("【{}】{}终止, reason=applicationId 非法, applicationId={}",
                    BIZ, ACTION_RESIGN_APPLICATION, applicationId);
            throw new RestClientException("Moka 标记候选人已离职 applicationId 非法: " + applicationId);
        }
        if (request == null) {
            log.info("【{}】{}终止, reason=请求体为空", BIZ, ACTION_RESIGN_APPLICATION);
            throw new RestClientException("Moka 标记候选人已离职请求体为空");
        }

        // —— 拼接 URL（替换路径占位符）——
        String url = mokaProperties.getBaseUrl()
                + MokaConstants.APPLICATION_RESIGN_PATH
                .replace(MokaConstants.APPLICATION_ID_PLACEHOLDER, String.valueOf(applicationId));

        MokaApplicationResignResponse body = mokaHttpClient.doPostOrPut(
                HttpMethod.PUT, url, request, BIZ, ACTION_RESIGN_APPLICATION, MokaApplicationResignResponse.class);

        if (body.isSuccess()) {
            log.info("【{}】{}成功, applicationId={}", BIZ, ACTION_RESIGN_APPLICATION, applicationId);
        } else {
            log.info("【{}】{}终止, applicationId={}, code={}, errorMessage={}",
                    BIZ, ACTION_RESIGN_APPLICATION, applicationId, body.getCode(), body.getErrorMessage());
        }
        return body;
    }
}
