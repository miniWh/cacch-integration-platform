package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateRecordsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateRecordsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 企业微信智能表格 HTTP 客户端
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComSmartSheetClient {

    private final RestTemplate restTemplate;

    /**
     * 查询智能表格子表列表
     */
    public WeComGetSheetResponse getSheets(String accessToken, WeComGetSheetRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_GET_SHEET_URL, accessToken);
        log.info("【WeComSmartSheet】查询子表, docid={}", request.getDocid());
        return post(url, request, WeComGetSheetResponse.class, "查询子表");
    }

    /**
     * 查询智能表格字段列表
     */
    public WeComGetFieldsResponse getFields(String accessToken, WeComGetFieldsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_GET_FIELDS_URL, accessToken);
        log.info("【WeComSmartSheet】查询字段, docid={}, sheetId={}", request.getDocid(), request.getSheetId());
        return post(url, request, WeComGetFieldsResponse.class, "查询字段");
    }

    /**
     * 查询智能表格记录列表
     */
    public WeComGetRecordsResponse getRecords(String accessToken, WeComGetRecordsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_GET_RECORDS_URL, accessToken);
        log.info("【WeComSmartSheet】查询记录, docid={}, sheetId={}", request.getDocid(), request.getSheetId());
        return post(url, request, WeComGetRecordsResponse.class, "查询记录");
    }

    /**
     * 添加智能表格记录
     */
    public WeComAddRecordsResponse addRecords(String accessToken, WeComAddRecordsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_ADD_RECORDS_URL, accessToken);
        log.info("【WeComSmartSheet】添加记录, docid={}, sheetId={}, count={}",
                request.getDocid(), request.getSheetId(), request.getRecords().size());
        return post(url, request, WeComAddRecordsResponse.class, "添加记录");
    }

    /**
     * 更新智能表格记录
     */
    public WeComUpdateRecordsResponse updateRecords(String accessToken, WeComUpdateRecordsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_UPDATE_RECORDS_URL, accessToken);
        log.info("【WeComSmartSheet】更新记录, docid={}, sheetId={}, count={}",
                request.getDocid(), request.getSheetId(), request.getRecords().size());
        return post(url, request, WeComUpdateRecordsResponse.class, "更新记录");
    }

    private <T> T post(String url, Object request, Class<T> responseType, String action) {
        try {
            T response = restTemplate.postForObject(url, request, responseType);
            if (response == null) {
                log.error("【WeComSmartSheet】{} 返回 null", action);
                throw new RestClientException("企业微信智能表格" + action + "返回 null");
            }
            return response;
        } catch (RestClientException e) {
            log.error("【WeComSmartSheet】{} HTTP 调用失败", action, e);
            throw e;
        }
    }
}
