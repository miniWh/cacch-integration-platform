package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaRoleListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Moka 自定义角色 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #listRoles()} — 获取 Moka 全量角色
 *     （GET {@value com.cacch.integration.common.constant.moka.MokaConstants#ROLE_LIST_PATH}?type=all</li>
 * </ul>
 *
 * <p>接口说明：
 * <ul>
 *     <li>查询参数 {@code type} 可选值：{@code all}（全部，含内建 + 自定义）、
 *     {@code custom}（仅自定义）、{@code builtin}（仅内建）。本 Client 固定传
 *     {@code type=all}，确保拉取完整角色列表。</li>
 *     <li>响应字段：{@code id}（角色 ID，对应 DB role_id）、
 *     {@code name}（角色名称，对应 DB role_name）、
 *     {@code role}（角色值，整数）、{@code description}（描述）。</li>
 * </ul>
 *
 * <p>HTTP 通用调用（Basic Auth、URI 构造、出入参日志、异常兜底）统一由
 * {@link MokaHttpClient} 承担，本类仅保留 URL 拼接、成功 INFO。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaRoleClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_LIST = "获取全量自定义角色";

    private final MokaHttpClient mokaHttpClient;
    private final MokaProperties mokaProperties;

    /**
     * 获取 Moka 全量角色 —— GET {@code /api-platform/v1/users/roles?type=all}
     *
     * <p>查询参数固定传 {@code type=all}（全部角色，含内建 + 自定义）。
     * Moka 侧角色数量通常较少（一般 ≤ 50），全量拉取无性能问题。</p>
     *
     * @return Moka 角色列表响应（data 为角色项列表，字段：id / name / role / description）
     */
    public MokaRoleListResponse listRoles() {
        String url = mokaProperties.getBaseUrl() + MokaConstants.ROLE_LIST_PATH + "?type=all";

        MokaRoleListResponse body = mokaHttpClient.doGet(url, BIZ, ACTION_LIST, MokaRoleListResponse.class);

        if (body.isSuccess()) {
            int roleCount = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, roleCount={}", BIZ, ACTION_LIST, roleCount);
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_LIST, body.getCode(), body.getMsg());
        }
        return body;
    }
}
