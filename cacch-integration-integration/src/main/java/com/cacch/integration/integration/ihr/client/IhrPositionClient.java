package com.cacch.integration.integration.ihr.client;

import com.cacch.integration.common.config.ihr.IhrProperties;
import com.cacch.integration.common.constant.ihr.IhrConstants;
import com.cacch.integration.integration.ihr.client.dto.IhrPositionListResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * IHR 职位 HTTP 客户端 — 「获取公司职位清单」
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrPositionClient {

    private static final String BIZ = IhrConstants.LOG_BIZ;
    private static final String ACTION_LIST_POSITIONS = "获取公司职位清单";

    private final RestTemplateHttpClient httpClient;
    private final IhrProperties ihrProperties;

    /**
     * 拉取全量职位清单（iHR 无分页，一次返回全部）
     */
    public IhrPositionListResponse listAll(String accessToken) {
        String url = ihrProperties.getBaseUrl() + IhrConstants.ORG_POSITION_LIST_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        IhrPositionListResponse body = httpClient.doGet(url, headers, BIZ, ACTION_LIST_POSITIONS, IhrPositionListResponse.class);

        if (body.isSuccess()) {
            int count = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, code={}, currentSize={}", BIZ, ACTION_LIST_POSITIONS, body.getCode(), count);
        } else {
            log.info("【{}】{}终止, code={}, message={}", BIZ, ACTION_LIST_POSITIONS, body.getCode(), body.getMessage());
        }
        return body;
    }
}
