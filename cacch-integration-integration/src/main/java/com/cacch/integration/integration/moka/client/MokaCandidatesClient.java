package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Moka 候选人 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #listApplications(MokaApplicationListRequest)} — 根据条件查询申请信息
 *         （POST /api-platform/v3/applications/list_by_condition，成功码 code=200）</li>
 * </ul>
 *
 * <p>HTTP 通用调用（Basic Auth、URI 构造、出入参日志、异常兜底）统一由
 * {@link MokaHttpClient} 承担，本类仅保留 URL 拼接、成功 INFO。</p>
 *
 * <p>历史：listApplications 原在 {@link MokaOrgClient}，2026-10 迁入本类，
 * 作为候选人域（Candidates）的唯一 Client。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaCandidatesClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_LIST_APPLICATIONS = "根据条件查询申请信息";

    private final MokaHttpClient mokaHttpClient;
    private final MokaProperties mokaProperties;

    /**
     * 根据条件查询申请信息（分页）—— 调用 Moka {@code POST /api-platform/v3/applications/list_by_condition}
     *
     * <p>支持按申请 id、候选人 id、职位 id、流程、阶段、招聘模式、归档状态、时间范围等条件查询。
     * 分页通过 {@code limit}（默认 20，最大 20）和 {@code next}（游标）实现。</p>
     *
     * <p>注意：成功码为 200（与部门接口的 code=0 不同）。</p>
     *
     * @param request 查询条件（所有字段均可选，空对象表示返回全部）
     * @return 申请分页响应（含 applicationList + next 分页游标）
     */
    public MokaApplicationListResponse listApplications(MokaApplicationListRequest request) {
        String url = mokaProperties.getBaseUrl() + MokaConstants.APPLICATION_LIST_PATH;

        MokaApplicationListResponse body = mokaHttpClient.doPostOrPut(
                HttpMethod.POST, url, request, BIZ, ACTION_LIST_APPLICATIONS, MokaApplicationListResponse.class);

        if (body.isSuccess()) {
            int listSize = body.getData() == null || body.getData().getApplicationList() == null
                    ? 0 : body.getData().getApplicationList().size();
            String next = body.getData() == null ? null : body.getData().getNext();
            log.info("【{}】{}成功, listSize={}, next={}", BIZ, ACTION_LIST_APPLICATIONS, listSize, next);
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_LIST_APPLICATIONS, body.getCode(), body.getMsg());
        }
        return body;
    }
}
