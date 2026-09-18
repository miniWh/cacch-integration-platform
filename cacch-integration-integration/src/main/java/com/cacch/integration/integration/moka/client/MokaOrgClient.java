package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaDeptListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncResponse;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * Moka 组织架构 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #syncDepartmentsFull(MokaDeptSyncRequest)} — 组织架构全量同步（PUT /api-platform/v2/departments）</li>
 *     <li>{@link #getDepartments(String)} — 获取全量组织架构（GET /api-platform/v1/departments）</li>
 * </ul>
 *
 * <p>鉴权：HTTP Basic Auth，将机构 API Key 作为 username（password 为空），
 * 即 {@code Authorization: Basic Base64(apiKey + ":")}。API Key 经配置属性
 * {@link MokaProperties#getApiKey()} 注入，严禁记入日志。</p>
 *
 * <p>网关地址取自 {@link MokaProperties#getBaseUrl()}（yml {@code moka.base-url}），
 * 与 {@link MokaConstants} 中的路径常量在运行时拼接；代码中不硬编码任何环境地址，
 * 测试 / 生产域名切换只改配置，无需重新打包。</p>
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

    private final RestTemplate restTemplate;
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
     * @param request 同步请求体（departments 列表必填；operatorEmail 可选）
     * @return Moka 同步结果响应（含 new / update / delete 数量）
     * @throws RestClientException API Key 未配置、URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public MokaDeptSyncResponse syncDepartmentsFull(MokaDeptSyncRequest request) {
        // —— 前置守卫：API Key 必须已配置 ——
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置，请在 yml 或环境变量 MOKA_API_KEY 中设置", BIZ, ACTION_FULL_SYNC);
            throw new RestClientException("Moka API Key 未配置");
        }

        if (request == null || request.getDepartments() == null) {
            log.info("【{}】{}终止, reason=请求体或 departments 为空", BIZ, ACTION_FULL_SYNC);
            throw new RestClientException("Moka 组织架构全量同步请求体为空");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.DEPT_FULL_SYNC_PATH;
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            log.info("【{}】{}终止, reason=URL 非法, url={}", BIZ, ACTION_FULL_SYNC, url);
            throw new RestClientException("Moka 组织架构全量同步 URL 非法: " + url, e);
        }

        // —— 构造 Basic Auth Header（apiKey 作为 username，password 为空）——
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((mokaProperties.getApiKey() + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth);

        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION_FULL_SYNC, uri.toString(), request);

        try {
            HttpEntity<MokaDeptSyncRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<MokaDeptSyncResponse> response = restTemplate.exchange(
                    uri, HttpMethod.PUT, entity, MokaDeptSyncResponse.class);
            MokaDeptSyncResponse body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION_FULL_SYNC, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, ACTION_FULL_SYNC);
                throw new RestClientException("Moka 组织架构全量同步响应为空");
            }

            if (body.isSuccess()) {
                Integer newCount = body.getNewCount();
                Integer updateCount = body.getUpdateCount();
                Integer deleteCount = body.getDeleteCount();
                log.info("【{}】{}成功, new={}, update={}, delete={}",
                        BIZ, ACTION_FULL_SYNC, newCount, updateCount, deleteCount);
            } else {
                log.info("【{}】{}终止, code={}, msg={}",
                        BIZ, ACTION_FULL_SYNC, body.getCode(), body.getMsg());
            }
            return body;

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, ACTION_FULL_SYNC, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, ACTION_FULL_SYNC, e);
            throw e;
        }
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
        // —— 前置守卫：API Key 必须已配置 ——
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置，请在 yml 或环境变量 MOKA_API_KEY 中设置",
                    BIZ, ACTION_GET_DEPTS);
            throw new RestClientException("Moka API Key 未配置");
        }

        // —— 拼接 URL（含可选 query 参数 updateTimeStart）——
        String url = mokaProperties.getBaseUrl() + MokaConstants.DEPT_LIST_PATH;
        if (StringUtils.hasText(updateTimeStart)) {
            url += "?updateTimeStart=" + updateTimeStart;
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            log.info("【{}】{}终止, reason=URL 非法, url={}", BIZ, ACTION_GET_DEPTS, url);
            throw new RestClientException("获取全量组织架构 URL 非法: " + url, e);
        }

        // —— 构造 Basic Auth Header（apiKey 作为 username，password 为空）——
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((mokaProperties.getApiKey() + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth);

        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION_GET_DEPTS, uri.toString(), null);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<MokaDeptListResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, MokaDeptListResponse.class);
            MokaDeptListResponse body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION_GET_DEPTS, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, ACTION_GET_DEPTS);
                throw new RestClientException("获取全量组织架构响应为空");
            }

            if (body.isSuccess()) {
                int deptCount = body.getData() == null ? 0 : body.getData().size();
                log.info("【{}】{}成功, deptCount={}", BIZ, ACTION_GET_DEPTS, deptCount);
            } else {
                log.info("【{}】{}终止, code={}, msg={}",
                        BIZ, ACTION_GET_DEPTS, body.getCode(), body.getMsg());
            }
            return body;

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, ACTION_GET_DEPTS, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, ACTION_GET_DEPTS, e);
            throw e;
        }
    }
}
