package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocRequest;
import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 企业微信文档 HTTP 客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComDocClient {

    private final RestTemplate restTemplate;

    /**
     * 新建文档/智能表格
     */
    public WeComCreateDocResponse createDoc(String accessToken, WeComCreateDocRequest request) {
        String url = String.format(WeComConstants.DOC_CREATE_URL, accessToken);
        log.info("【WeComDoc】新建文档, docName={}, docType={}", request.getDocName(), request.getDocType());
        return post(url, request, WeComCreateDocResponse.class, "新建文档");
    }

    private <T> T post(String url, Object request, Class<T> responseType, String action) {
        try {
            T response = restTemplate.postForObject(url, request, responseType);
            if (response == null) {
                log.error("【WeComDoc】{} 返回 null", action);
                throw new RestClientException("企业微信" + action + "返回 null");
            }
            return response;
        } catch (RestClientException e) {
            log.error("【WeComDoc】{} HTTP 调用失败", action, e);
            throw e;
        }
    }
}
