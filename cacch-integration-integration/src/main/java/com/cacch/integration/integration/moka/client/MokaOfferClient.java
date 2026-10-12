package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoRequest;
import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoResponse;
import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoResponse.OfferExtendInfo;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Moka ATS —— 获取 offer 扩展信息 V3 HTTP 客户端
 *
 * <p>调用 POST {@code /api-platform/v3/getOfferExtendInfos}。
 * 提供批量查询方法：按 applicationIds 批量请求，返回 Map<applicationId, OfferExtendInfo>。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaOfferClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_GET_OFFER_EXTEND_INFO = "获取offer扩展信息(通用透传)";
    private static final String ACTION_GET_OFFER_EXTEND = "获取offer扩展信息(Step 1c专用翻页)";

    /**
     * 单批次最大请求 applicationIds 数量（Moka 接口限制 100）
     */
    private static final int BATCH_SIZE = 100;

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 获取 offer 扩展信息 —— 透传 Moka {@code POST /api-platform/v3/getOfferExtendInfos}
     *
     * <p>通用透传接口：调用方自行组装 MokaOfferExtendInfoRequest（orgId / applicationIds / offerAttachment），
     * 本方法仅负责单次 HTTP 执行与鉴权头构造，不做翻页、不做 Map 转换。</p>
     *
     * <p>与 Step 1c 专用的 {@link #getOfferExtendInfos(List)} 区别：
     * 后者内部自组装 request、按 BATCH_SIZE 翻页、返回 Map<applicationId, OfferExtendInfo>，用于批量补全；
     * 本方法直接透传请求体，适合 Service 层按需查询。</p>
     *
     * @param request Moka offer 扩展信息请求体，需含 orgId 与 applicationIds
     * @return Moka 原始响应体，调用方通过 response.isSuccess() 判定成功
     * @throws IllegalStateException Moka API Key 未配置时抛出
     */
    public MokaOfferExtendInfoResponse getOfferExtendInfo(MokaOfferExtendInfoRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_GET_OFFER_EXTEND_INFO);
            throw new IllegalStateException("Moka API Key 未配置");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.OFFER_EXTEND_INFO_PATH;
        HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

        MokaOfferExtendInfoResponse body = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers,
                BIZ, ACTION_GET_OFFER_EXTEND_INFO, MokaOfferExtendInfoResponse.class);

        if (body.isSuccess()) {
            int dataSize = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, dataSize={}", BIZ, ACTION_GET_OFFER_EXTEND_INFO, dataSize);
        } else {
            log.info("【{}】{}终止, code={}, msg={}", BIZ, ACTION_GET_OFFER_EXTEND_INFO, body.getCode(), body.getMsg());
        }
        return body;
    }

    /**
     * 按 applicationIds 批量查询 offer 扩展信息
     *
     * <p>Step 1c 补全：输入主表所有 applicationId，
     * 返回 Map<applicationId, OfferExtendInfo>，从中提取
     * checkInDate / offerDepartmentCode / offerDepartmentName / approvalStatus。</p>
     *
     * @param applicationIds 申请 ID 列表
     * @return applicationId → OfferExtendInfo 映射；查不到的不出现在 map
     */
    public Map<Long, OfferExtendInfo> getOfferExtendInfos(List<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            log.info("【{}】{}终止, reason=applicationIds 为空", BIZ, ACTION_GET_OFFER_EXTEND);
            return Collections.emptyMap();
        }
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_GET_OFFER_EXTEND);
            throw new IllegalStateException("Moka API Key 未配置");
        }

        Map<Long, OfferExtendInfo> result = new HashMap<>();
        String orgId = mokaProperties.getOrgId();

        // 分批查询（每批最多 100 条）
        for (int i = 0; i < applicationIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, applicationIds.size());
            List<Long> batch = applicationIds.subList(i, end);

            MokaOfferExtendInfoRequest request = new MokaOfferExtendInfoRequest();
            request.setOrgId(orgId);
            request.setApplicationIds(batch);

            String url = mokaProperties.getBaseUrl() + MokaConstants.OFFER_EXTEND_INFO_PATH;
            HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

            MokaOfferExtendInfoResponse response = httpClient.doPostOrPut(
                    HttpMethod.POST, url, request, headers,
                    BIZ, ACTION_GET_OFFER_EXTEND, MokaOfferExtendInfoResponse.class);

            // —— [DEBUG] 临时 raw 调用，打印 Moka 原始响应中 checkInDate 的真实值 ——
            debugRawCheckInDate(url, headers, request, batch);

            if (!response.isSuccess()) {
                log.info("【{}】{}终止, code={}, msg={}, batchStart={}",
                        BIZ, ACTION_GET_OFFER_EXTEND, response.getCode(), response.getMsg(), i);
                continue;
            }

            if (response.getData() != null) {
                for (OfferExtendInfo info : response.getData()) {
                    if (info.getApplicationId() != null) {
                        result.put(info.getApplicationId(), info);
                    }
                }
            }

            log.info("【{}】{}本批完成, batchSize={}, 已匹配={}",
                    BIZ, ACTION_GET_OFFER_EXTEND, batch.size(), result.size());
        }

        log.info("【{}】{}完成, 请求数={}, 成功匹配={}",
                BIZ, ACTION_GET_OFFER_EXTEND, applicationIds.size(), result.size());
        return result;
    }

    /**
     * [DEBUG] 临时方法 —— 用 RestTemplate 发一次 raw 请求，直接拿 Moka 原始响应字符串，
     * 提取每个 data 元素的 checkInDate / content / contactEmail 字段的真实值，
     * 与反序列化后 DTO 中的 null 对比，定位是 Moka 没返回还是 DTO 映射丢了。
     *
     * <p>失败时只打 WARN，不影响主流程。确认问题根因后删除本方法与调用点。</p>
     */
    private void debugRawCheckInDate(String url, HttpHeaders headers,
                                     MokaOfferExtendInfoRequest request, List<Long> batchIds) {
        try {
            ResponseEntity<String> rawResp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(request, headers), String.class);
            String rawBody = rawResp.getBody();
            if (rawBody == null) {
                log.warn("【{}】[DEBUG] raw 响应体为空, batchIds={}", BIZ, batchIds);
                return;
            }

            // 从原始 JSON 里搜索 checkInDate / content / contactEmail
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode dataNode = root.path("data");
            if (!dataNode.isArray()) {
                log.warn("【{}】[DEBUG] raw data 不是数组, 原始前500字符: {}", BIZ,
                        rawBody.length() > 500 ? rawBody.substring(0, 500) : rawBody);
                return;
            }

            for (JsonNode item : dataNode) {
                Long appId = item.path("applicationId").asLong(0L);
                // 直接从原始 JSON 取字段值（即使反序列化丢了这里也能看到）
                JsonNode checkInDate = item.path("checkInDate");
                JsonNode content = item.path("content");
                JsonNode contactEmail = item.path("contactEmail");
                JsonNode contactName = item.path("contactName");

                log.warn("【{}】[DEBUG] raw 字段值, applicationId={}, checkInDate(raw)={}, content长度={}, contactEmail(raw)={}, contactName(raw)={}",
                        BIZ, appId,
                        checkInDate.isMissingNode() ? "[MISSING]" : checkInDate.asText("[null]"),
                        content.isTextual() ? content.asText().length() : 0,
                        contactEmail.isMissingNode() ? "[MISSING]" : contactEmail.asText("[null]"),
                        contactName.isMissingNode() ? "[MISSING]" : contactName.asText("[null]"));
            }
        } catch (RestClientException e) {
            log.warn("【{}】[DEBUG] raw HTTP 调用失败, reason={}", BIZ, e.getMessage());
        } catch (Exception e) {
            log.warn("【{}】[DEBUG] raw 响应解析失败, reason={}", BIZ, e.getMessage());
        }
    }
}
