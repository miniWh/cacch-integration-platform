package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddFieldsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddSheetRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComDeleteFieldsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComDeleteFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateFieldsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateRecordsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateSheetRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateSheetResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 企业微信智能表格 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担；
 * 企微鉴权特殊：access_token 直接内嵌在 URL query 参数中，无需 Header。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComSmartSheetClient {

    private static final String BIZ = "WeComSmartSheet";

    private final RestTemplateHttpClient httpClient;

    public WeComGetSheetResponse getSheets(String accessToken, WeComGetSheetRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_GET_SHEET_URL, accessToken);
        return post(url, request, WeComGetSheetResponse.class, "查询子表");
    }

    public WeComGetFieldsResponse getFields(String accessToken, WeComGetFieldsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_GET_FIELDS_URL, accessToken);
        return post(url, request, WeComGetFieldsResponse.class, "查询字段");
    }

    public WeComGetRecordsResponse getRecords(String accessToken, WeComGetRecordsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_GET_RECORDS_URL, accessToken);
        return post(url, request, WeComGetRecordsResponse.class, "查询记录");
    }

    public WeComAddRecordsResponse addRecords(String accessToken, WeComAddRecordsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_ADD_RECORDS_URL, accessToken);
        return post(url, request, WeComAddRecordsResponse.class, "添加记录");
    }

    public WeComDeleteFieldsResponse deleteFields(String accessToken, WeComDeleteFieldsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_DELETE_FIELDS_URL, accessToken);
        return post(url, request, WeComDeleteFieldsResponse.class, "删除字段");
    }

    public WeComAddSheetResponse addSheet(String accessToken, WeComAddSheetRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_ADD_SHEET_URL, accessToken);
        return post(url, request, WeComAddSheetResponse.class, "添加子表");
    }

    public WeComUpdateSheetResponse updateSheet(String accessToken, WeComUpdateSheetRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_UPDATE_SHEET_URL, accessToken);
        return post(url, request, WeComUpdateSheetResponse.class, "更新子表");
    }

    public WeComAddFieldsResponse addFields(String accessToken, WeComAddFieldsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_ADD_FIELDS_URL, accessToken);
        return post(url, request, WeComAddFieldsResponse.class, "添加字段");
    }

    public WeComUpdateFieldsResponse updateFields(String accessToken, WeComUpdateFieldsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_UPDATE_FIELDS_URL, accessToken);
        return post(url, request, WeComUpdateFieldsResponse.class, "更新字段");
    }

    public WeComUpdateRecordsResponse updateRecords(String accessToken, WeComUpdateRecordsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_UPDATE_RECORDS_URL, accessToken);
        return post(url, request, WeComUpdateRecordsResponse.class, "更新记录");
    }

    private <T> T post(String url, Object request, Class<T> responseType, String action) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // ThirdPartyHttpLogSupport 日志已由 RestTemplateHttpClient 统一输出，此处补成功 INFO
        // （RestTemplateHttpClient 输出的是通用出入参日志，业务成功判定 + 统计项仍在各 Client）
        return httpClient.doPostOrPut(HttpMethod.POST, url, request, headers, BIZ, action, responseType);
    }
}
