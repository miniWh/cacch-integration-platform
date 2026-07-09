package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddSheetRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateSheetRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddFieldsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComDeleteFieldsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComDeleteFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsResponse;
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
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     查询请求（含 docid），不可为空
     * @return 子表列表响应
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComGetSheetResponse getSheets(String accessToken, WeComGetSheetRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_GET_SHEET_URL, accessToken);
        log.info("【WeComSmartSheet】查询子表, docid={}", request.getDocid());
        return post(url, request, WeComGetSheetResponse.class, "查询子表");
    }

    /**
     * 查询智能表格字段列表
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     查询请求（含 docid、sheetId），不可为空
     * @return 字段列表响应
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComGetFieldsResponse getFields(String accessToken, WeComGetFieldsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_GET_FIELDS_URL, accessToken);
        log.info("【WeComSmartSheet】查询字段, docid={}, sheetId={}", request.getDocid(), request.getSheetId());
        return post(url, request, WeComGetFieldsResponse.class, "查询字段");
    }

    /**
     * 查询智能表格记录列表
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     查询请求（含 docid、sheetId、分页），不可为空
     * @return 记录列表响应；无数据时 records 可能为 null
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComGetRecordsResponse getRecords(String accessToken, WeComGetRecordsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_GET_RECORDS_URL, accessToken);
        log.info("【WeComSmartSheet】查询记录, docid={}, sheetId={}", request.getDocid(), request.getSheetId());
        return post(url, request, WeComGetRecordsResponse.class, "查询记录");
    }

    /**
     * 添加智能表格记录
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     添加记录请求，不可为空
     * @return 添加结果（含新建 recordId）
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComAddRecordsResponse addRecords(String accessToken, WeComAddRecordsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_ADD_RECORDS_URL, accessToken);
        log.info("【WeComSmartSheet】添加记录, docid={}, sheetId={}, count={}",
                request.getDocid(), request.getSheetId(), request.getRecords().size());
        return post(url, request, WeComAddRecordsResponse.class, "添加记录");
    }

    /**
     * 删除智能表格字段
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     删除字段请求，不可为空
     * @return 删除结果响应
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComDeleteFieldsResponse deleteFields(String accessToken, WeComDeleteFieldsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_DELETE_FIELDS_URL, accessToken);
        log.info("【WeComSmartSheet】删除字段, docid={}, sheetId={}, count={}",
                request.getDocid(), request.getSheetId(), request.getFieldIds().size());
        return post(url, request, WeComDeleteFieldsResponse.class, "删除字段");
    }

    /**
     * 添加智能表格子表
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     添加子表请求，不可为空
     * @return 添加结果（含 sheetId）
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComAddSheetResponse addSheet(String accessToken, WeComAddSheetRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_ADD_SHEET_URL, accessToken);
        log.info("【WeComSmartSheet】添加子表, docid={}, title={}",
                request.getDocid(), request.getProperties() != null ? request.getProperties().getTitle() : null);
        return post(url, request, WeComAddSheetResponse.class, "添加子表");
    }

    /**
     * 更新智能表格子表（重命名等）
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     更新子表请求，不可为空
     * @return 更新结果响应
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComUpdateSheetResponse updateSheet(String accessToken, WeComUpdateSheetRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_UPDATE_SHEET_URL, accessToken);
        log.info("【WeComSmartSheet】更新子表, docid={}, sheetId={}",
                request.getDocid(),
                request.getProperties() != null ? request.getProperties().getSheetId() : null);
        return post(url, request, WeComUpdateSheetResponse.class, "更新子表");
    }

    /**
     * 添加智能表格字段
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     添加字段请求，不可为空
     * @return 添加结果响应
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComAddFieldsResponse addFields(String accessToken, WeComAddFieldsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_ADD_FIELDS_URL, accessToken);
        log.info("【WeComSmartSheet】添加字段, docid={}, sheetId={}, count={}",
                request.getDocid(), request.getSheetId(), request.getFields().size());
        return post(url, request, WeComAddFieldsResponse.class, "添加字段");
    }

    /**
     * 更新智能表格字段（重命名列等）
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     更新字段请求，不可为空
     * @return 更新结果响应
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComUpdateFieldsResponse updateFields(String accessToken, WeComUpdateFieldsRequest request) {
        String url = String.format(WeComConstants.SMARTSHEET_UPDATE_FIELDS_URL, accessToken);
        log.info("【WeComSmartSheet】更新字段, docid={}, sheetId={}, count={}",
                request.getDocid(), request.getSheetId(), request.getFields().size());
        return post(url, request, WeComUpdateFieldsResponse.class, "更新字段");
    }

    /**
     * 更新智能表格记录
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     更新记录请求，不可为空
     * @return 更新结果响应
     * @throws RestClientException 网络错误或接口返回 null
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
                log.info("【WeComSmartSheet】{}终止, reason=接口返回null", action);
                log.error("【WeComSmartSheet】{} 返回 null", action);
                throw new RestClientException("企业微信智能表格" + action + "返回 null");
            }
            return response;
        } catch (RestClientException e) {
            log.info("【WeComSmartSheet】{}终止, reason={}", action, e.getMessage());
            log.error("【WeComSmartSheet】{} HTTP 调用失败", action, e);
            throw e;
        }
    }
}
