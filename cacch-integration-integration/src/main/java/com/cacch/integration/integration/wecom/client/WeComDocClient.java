package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocRequest;
import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 企业微信文档 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComDocClient {

    private static final String BIZ = "WeComDoc";

    private final RestTemplateHttpClient httpClient;

    /**
     * 新建文档/智能表格
     */
    public WeComCreateDocResponse createDoc(String accessToken, WeComCreateDocRequest request) {
        String url = String.format(WeComConstants.DOC_CREATE_URL, accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return httpClient.doPostOrPut(HttpMethod.POST, url, request, headers, BIZ, "新建文档", WeComCreateDocResponse.class);
    }
}
