package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaDeptListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

/**
 * Moka 组织架构 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #syncDepartmentsFull(MokaDeptSyncRequest)} — 组织架构全量同步（PUT /api-platform/v2/departments）</li>
 *     <li>{@link #getDepartments(String)} — 获取全量组织架构（GET /api-platform/v1/departments）</li>
 * </ul>
 *
 * <p>HTTP 通用调用（Basic Auth、URI 构造、出入参日志、异常兜底）统一由
 * {@link MokaHttpClient} 承担，本类仅保留 URL 拼接、方法级前置校验、成功 INFO。</p>
 *
 * <p>网关地址取自 {@link MokaProperties#getBaseUrl()}（yml {@code moka.base-url}），
 * 与 {@link MokaConstants} 中的路径常量在运行时拼接。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaOrgClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_FULL_SYNC = "组织架构全量同步";
    private static final String ACTION_GET_DEPTS = "获取全量组织架构";

    private final MokaHttpClient mokaHttpClient;
    private final MokaProperties mokaProperties;

    /**
     * 组织架构全量同步 —— 以 departmentCode 为主键对比，执行新增 / 更新 / 标记删除
     *
     * <p>同步语义：
     * <ul>
     *     <li>系统没有、同步时有 → 新增部门</li>
     *     <li>两边都有 → 更新已存在的部门信息；若系统内已标记删除则恢复为正常</li>
     *     <li>系统有、同步时没有 → 部门标记为已删除（需手动进入 Moka 后台合并删除）</li>
     * </ul>
     *
     * @param request 同步请求体（departments 列表必填）
     * @return Moka 同步结果响应（含 new / update / delete 数量）
     * @throws RestClientException API Key 未配置、请求体非法、URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public MokaDeptSyncResponse syncDepartmentsFull(MokaDeptSyncRequest request) {
        // —— 方法级前置校验（MokaHttpClient 只做 API Key 守卫）——
        if (request == null || request.getDepartments() == null) {
            log.info("【{}】{}终止, reason=请求体或 departments 为空", BIZ, ACTION_FULL_SYNC);
            throw new RestClientException("Moka 组织架构全量同步请求体为空");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.DEPT_FULL_SYNC_PATH;
        log.info("【{}】开始{}, deptCount={}", BIZ, ACTION_FULL_SYNC, request.getDepartments().size());

        MokaDeptSyncResponse body = mokaHttpClient.doPostOrPut(
                HttpMethod.PUT, url, request, BIZ, ACTION_FULL_SYNC, MokaDeptSyncResponse.class);

        // —— 成功 INFO（保留在 Client，各接口统计项不同）——
        if (body.isSuccess()) {
            log.info("【{}】{}成功, new={}, update={}, delete={}",
                    BIZ, ACTION_FULL_SYNC, body.getNewCount(), body.getUpdateCount(), body.getDeleteCount());
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_FULL_SYNC, body.getCode(), body.getMsg());
        }
        return body;
    }

    /**
     * 获取全量组织架构 —— 调用 Moka {@code GET /api-platform/v1/departments}
     *
     * <p>返回 Moka 侧全量部门列表。支持可选 {@code updateTimeStart} 增量查询参数
     * （格式 {@code yyyy-MM-dd HH:mm:ss}），为空时返回全量数据。</p>
     *
     * @param updateTimeStart 增量查询起始时间（可选，格式 yyyy-MM-dd HH:mm:ss）；
     *                        为空或空白时返回全量部门
     * @return Moka 全量组织架构响应（data 为部门列表）
     * @throws RestClientException API Key 未配置、URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public MokaDeptListResponse getDepartments(String updateTimeStart) {
        // —— 拼接 URL（含可选 query 参数 updateTimeStart）——
        String url = mokaProperties.getBaseUrl() + MokaConstants.DEPT_LIST_PATH;
        if (StringUtils.hasText(updateTimeStart)) {
            url += "?updateTimeStart=" + updateTimeStart;
        }

        MokaDeptListResponse body = mokaHttpClient.doGet(url, BIZ, ACTION_GET_DEPTS, MokaDeptListResponse.class);

        if (body.isSuccess()) {
            int deptCount = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, deptCount={}", BIZ, ACTION_GET_DEPTS, deptCount);
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_GET_DEPTS, body.getCode(), body.getMsg());
        }
        return body;
    }
}
