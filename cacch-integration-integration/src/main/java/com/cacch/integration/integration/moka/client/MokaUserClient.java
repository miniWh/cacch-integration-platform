package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaUserSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaUserSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Moka 用户信息 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #syncUserInfo(MokaUserSyncRequest)} — 用户信息同步
 *     （POST {@value com.cacch.integration.common.constant.moka.MokaConstants#USER_SYNC_INFO_PATH} </li>
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
public class MokaUserClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_SYNC = "Moka用户信息同步";

    private final MokaHttpClient mokaHttpClient;
    private final MokaProperties mokaProperties;

    /**
     * 用户信息同步 —— 以手机号为唯一键执行 upsert（存在则更新、不存在则创建）
     *
     * <p>Moka API 单次推送上限 100 条，调用方（Manager 层）须自行分批。
     * 响应只反馈整批调用结果，不返回单个用户的处理状态，
     * 因此本 Client 视整批成功为成功、整批失败为失败。</p>
     *
     * @param request 同步请求体（usersInfo 必填且非空）
     * @return Moka 同步结果响应
     * @throws RestClientException 请求体非法时抛出（API Key / HTTP 异常由 MokaHttpClient 抛出）
     */
    public MokaUserSyncResponse syncUserInfo(MokaUserSyncRequest request) {
        // —— 方法级前置校验（MokaHttpClient 只做 API Key 守卫）——
        if (request == null || request.getUsersInfo() == null || request.getUsersInfo().isEmpty()) {
            log.info("【{}】{}终止, reason=请求体或 usersInfo 为空", BIZ, ACTION_SYNC);
            throw new RestClientException("Moka 用户信息同步请求体为空");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.USER_SYNC_INFO_PATH;

        MokaUserSyncResponse body = mokaHttpClient.doPostOrPut(
                HttpMethod.POST, url, request, BIZ, ACTION_SYNC, MokaUserSyncResponse.class);

        if (body.isSuccess()) {
            int userCount = request.getUsersInfo().size();
            log.info("【{}】{}成功, userCount={}", BIZ, ACTION_SYNC, userCount);
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_SYNC, body.getCode(), body.getMsg());
        }
        return body;
    }
}
