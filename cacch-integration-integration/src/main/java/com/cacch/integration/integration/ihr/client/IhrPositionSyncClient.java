package com.cacch.integration.integration.ihr.client;

import com.cacch.integration.common.config.ihr.IhrProperties;
import com.cacch.integration.common.constant.ihr.IhrConstants;
import com.cacch.integration.common.constant.ihr.IhrPathConstants;
import com.cacch.integration.integration.ihr.client.dto.IhrPositionIdsResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrPositionListResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

/**
 * IHR 岗位主数据（MDM）同步链路 HTTP 客户端 —— 岗位域新链路聚合 Client
 *
 * <p>承载岗位 MDM 链路的两个接口：「获取公司职位清单」（全量，复用既有
 * {@code ORG_POSITION_LIST_PATH} 常量）与「根据职位 id 获取职位清单」（按 ID 批量回源）。
 * 旧链路 {@link IhrPositionClient} 保持不动，新旧并行。</p>
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担（含出入参日志）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrPositionSyncClient {

    private static final String BIZ = IhrConstants.LOG_BIZ;
    private static final String ACTION_LIST_POSITIONS = "获取公司职位清单";
    private static final String ACTION_LIST_POSITIONS_BY_IDS = "根据职位id获取职位清单";

    private final RestTemplateHttpClient httpClient;
    private final IhrProperties ihrProperties;

    /**
     * 拉取全量职位清单（iHR 无分页，一次返回全部）
     *
     * @param accessToken IHR access_token（来自 Token 服务缓存），不可为空
     * @return 全量职位清单响应；业务成功以 {@link IhrPositionListResponse#isSuccess()} 判定
     * @throws RestClientException 网络或 HTTP 非 2xx、响应体反序列化失败时抛出
     */
    public IhrPositionListResponse listAll(String accessToken) {
        String fullUrl = ihrProperties.getBaseUrl() + IhrConstants.ORG_POSITION_LIST_PATH;

        HttpHeaders headers = buildBearerHeaders(accessToken);
        IhrPositionListResponse body = httpClient.doGet(fullUrl, headers, BIZ, ACTION_LIST_POSITIONS,
                IhrPositionListResponse.class);

        if (body.isSuccess()) {
            int count = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, code={}, currentSize={}", BIZ, ACTION_LIST_POSITIONS, body.getCode(), count);
        } else {
            log.info("【{}】{}终止, code={}, message={}", BIZ, ACTION_LIST_POSITIONS, body.getCode(), body.getMessage());
        }
        return body;
    }

    /**
     * 根据职位 id 集合批量获取职位清单
     *
     * @param accessToken  IHR access_token（来自 Token 服务缓存），不可为空
     * @param positionIds  职位 ID 集合（iHR 职位 uuid；非空；单批建议 ≤100，由上层 Service 分批）
     * @return 职位清单响应（iHR 侧不存在的 ID 无对应元素）；业务成功以 {@link IhrPositionIdsResponse#isSuccess()} 判定
     * @throws RestClientException 职位 ID 集合为空、网络或 HTTP 非 2xx、响应体反序列化失败时抛出
     */
    public IhrPositionIdsResponse listByIds(String accessToken, List<String> positionIds) {
        if (positionIds == null || positionIds.isEmpty()) {
            throw new RestClientException("IHR 根据职位id获取职位清单 positionIds 为空");
        }

        String fullUrl = ihrProperties.getBaseUrl() + IhrPathConstants.ORG_POSITION_LIST_BY_IDS_PATH;
        log.info("【{}】{}, 完整url={}, positionIdCount={}", BIZ, ACTION_LIST_POSITIONS_BY_IDS, fullUrl, positionIds.size());
        HttpHeaders headers = buildBearerHeaders(accessToken);

        IhrPositionIdsResponse body = httpClient.doPostOrPut(HttpMethod.POST, fullUrl, positionIds, headers,
                BIZ, ACTION_LIST_POSITIONS_BY_IDS, IhrPositionIdsResponse.class);

        if (body.isSuccess()) {
            int count = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, code=0, positionIdCount={}, returnCount={}",
                    BIZ, ACTION_LIST_POSITIONS_BY_IDS, positionIds.size(), count);
        } else {
            log.info("【{}】{}终止, positionIdCount={}, code={}, message={}", BIZ, ACTION_LIST_POSITIONS_BY_IDS,
                    positionIds.size(), body.getCode(), body.getMessage());
        }
        return body;
    }

    /**
     * 构造 Bearer 鉴权头（JSON 请求体场景）
     */
    private HttpHeaders buildBearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return headers;
    }
}
