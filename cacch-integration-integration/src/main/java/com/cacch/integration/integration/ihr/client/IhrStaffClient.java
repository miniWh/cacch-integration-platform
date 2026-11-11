package com.cacch.integration.integration.ihr.client;

import com.cacch.integration.common.config.ihr.IhrProperties;
import com.cacch.integration.common.constant.ihr.IhrPathConstants;
import com.cacch.integration.common.constant.ihr.IhrStaffConstants;
import com.cacch.integration.common.constant.ihr.IhrStaffLogConstants;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffIdsResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;

/**
 * IHR 员工域 HTTP 客户端 — 员工类所有接口调用的聚合类
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担。
 * 当前已封装「获取员工 ID 清单」接口，后续员工域接口（如查询员工基础信息）
 * 继续在本类新增方法，不新建 Client 文件。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrStaffClient {

    private static final String BIZ = IhrStaffLogConstants.LOG_BIZ;
    private static final String ACTION_STAFF_IDS_LIST = IhrStaffLogConstants.buildAction("获取员工ID清单");
    private static final String ACTION_STAFF_BASIC = IhrStaffLogConstants.buildAction("获取员工基本信息");

    /**
     * 静态 ObjectMapper —— 将原始响应报文反序列化为 {@link IhrStaffBasicResponse}
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final RestTemplateHttpClient httpClient;
    private final IhrProperties ihrProperties;

    /**
     * 单页拉取员工 ID 清单
     *
     * @param accessToken iHR access_token
     * @param staffStatus 员工状态过滤，取值 {@link IhrStaffConstants#STAFF_STATUS_IN_SERVICE}（在职）
     *                    / {@link IhrStaffConstants#STAFF_STATUS_QUIT}（离职）；传 null 或空字符串查全部状态
     * @param pageNo      拉取页码（从 1 起）
     * @return iHR 原始响应体；业务成功以 {@link IhrStaffIdsResponse#isSuccess()} 判定
     * @throws org.springframework.web.client.RestClientException HTTP 调用异常时抛出
     */
    public IhrStaffIdsResponse fetchStaffIdsPage(String accessToken, String staffStatus, Integer pageNo) {
        StringBuilder urlBuilder = new StringBuilder(ihrProperties.getBaseUrl() + IhrPathConstants.STAFF_IDS_LIST_PATH);
        char sep = '?';
        if (staffStatus != null && !staffStatus.isEmpty()) {
            urlBuilder.append(sep).append("staffStatus=").append(staffStatus);
            sep = '&';
        }
        if (pageNo != null && pageNo >= 1) {
            urlBuilder.append(sep).append("pageNo=").append(pageNo);
        }
        String fullUrl = urlBuilder.toString();
        log.info("【{}】{}, 完整url={}", BIZ, ACTION_STAFF_IDS_LIST, fullUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        IhrStaffIdsResponse body = httpClient.doGet(fullUrl, headers, BIZ, ACTION_STAFF_IDS_LIST,
                IhrStaffIdsResponse.class);

        if (body.isSuccess()) {
            int size = body.getData() == null || body.getData().getDataList() == null
                    ? 0 : body.getData().getDataList().size();
            log.info("【{}】{}成功, code=0, pageNo={}, 本页条数={}", BIZ, ACTION_STAFF_IDS_LIST, pageNo, size);
        } else {
            log.info("【{}】{}终止, code={}, message={}", BIZ, ACTION_STAFF_IDS_LIST, body.getCode(), body.getMessage());
        }
        return body;
    }

    /**
     * 按 staffId 查询单个员工的基本信息
     *
     * @param accessToken iHR access_token
     * @param staffId     员工 ID（iHR 返回的 UUID，最大 36 位）
     * @return iHR 原始响应体（含 rawJson 原文）；业务成功以 {@link IhrStaffBasicResponse#isSuccess()} 判定
     * @throws RestClientException HTTP 调用异常或响应体反序列化失败时抛出
     */
    public IhrStaffBasicResponse fetchStaffBasic(String accessToken, String staffId) {
        String fullUrl = ihrProperties.getBaseUrl() + String.format(IhrPathConstants.STAFF_BASIC_PATH, staffId);
        log.info("【{}】{}, 完整url={}", BIZ, ACTION_STAFF_BASIC, fullUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        String rawBody = httpClient.doGet(fullUrl, headers, BIZ, ACTION_STAFF_BASIC, String.class);

        IhrStaffBasicResponse body;
        try {
            body = MAPPER.readValue(rawBody, IhrStaffBasicResponse.class);
        } catch (Exception e) {
            log.info("【{}】{}终止, reason=响应体反序列化失败", BIZ, ACTION_STAFF_BASIC);
            log.error("【{}】{} 响应体反序列化失败", BIZ, ACTION_STAFF_BASIC, e);
            throw new RestClientException(BIZ + " " + ACTION_STAFF_BASIC + " 响应体反序列化失败: " + e.getMessage(), e);
        }
        body.setRawJson(rawBody);

        if (body.isSuccess()) {
            boolean hasData = body.getData() != null;
            log.info("【{}】{}成功, code=0, staffId={}, hasData={}", BIZ, ACTION_STAFF_BASIC, staffId, hasData);
        } else {
            log.info("【{}】{}终止, code={}, message={}", BIZ, ACTION_STAFF_BASIC, body.getCode(), body.getMessage());
        }
        return body;
    }
}
