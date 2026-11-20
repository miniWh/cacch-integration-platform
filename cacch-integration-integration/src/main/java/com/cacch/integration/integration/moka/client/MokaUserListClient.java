package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaUserListV3Request;
import com.cacch.integration.integration.moka.client.dto.MokaUserListV3Response;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Moka 获取人事信息 V3 HTTP 客户端
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaUserListClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION = "Moka获取人事信息V3";

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

  /**
   * 分页查询人事用户列表（HTTP Basic Auth，与 syncInfo 相同 API Key）
   *
   * @param request 分页请求
   * @return V3 响应
   */
  public MokaUserListV3Response listUsers(MokaUserListV3Request request) {
    if (!mokaProperties.isApiKeyConfigured()) {
      log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION);
      throw new RestClientException("Moka API Key 未配置");
    }
    String url = mokaProperties.getBaseUrl() + MokaConstants.USER_LIST_V3_PATH;
    HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

    MokaUserListV3Response body = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers, BIZ, ACTION, MokaUserListV3Response.class);

        if (body != null && body.isSuccess()) {
            int size = body.getData() == null || body.getData().getUserList() == null
                    ? 0 : body.getData().getUserList().size();
            log.info("【{}】{}成功, pageSize={}, nextUserId={}",
                    BIZ, ACTION, size,
                    body.getData() != null ? body.getData().getNextUserId() : null);
        } else if (body != null) {
            log.info("【{}】{}终止, code={}, msg={}", BIZ, ACTION, body.getCode(), body.getMsg());
        }
        return body;
    }
}