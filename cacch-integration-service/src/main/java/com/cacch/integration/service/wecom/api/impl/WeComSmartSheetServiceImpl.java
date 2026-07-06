package com.cacch.integration.service.wecom.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.WeComSmartSheetClient;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordWriteItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateRecordsRequest;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUpdateRecordsResponse;
import com.cacch.integration.service.wecom.api.IWeComSmartSheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 企业微信智能表格服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComSmartSheetServiceImpl implements IWeComSmartSheetService {

    private static final String KEY_TYPE_FIELD_ID = "CELL_VALUE_KEY_TYPE_FIELD_ID";

    private final WeComSmartSheetClient weComSmartSheetClient;

    @Override
    public WeComGetSheetResponse getSheets(String accessToken, String docId, String sheetId, Boolean needAllTypeSheet) {
        WeComGetSheetRequest request = WeComGetSheetRequest.builder()
                .docid(docId)
                .sheetId(sheetId)
                .needAllTypeSheet(needAllTypeSheet)
                .build();
        WeComGetSheetResponse response = weComSmartSheetClient.getSheets(accessToken, request);
        assertWeComSuccess(response, "查询子表");
        return response;
    }

    @Override
    public WeComGetFieldsResponse getFields(String accessToken, String docId, String sheetId,
                                              Integer offset, Integer limit) {
        WeComGetFieldsRequest request = WeComGetFieldsRequest.builder()
                .docid(docId)
                .sheetId(sheetId)
                .offset(offset)
                .limit(limit)
                .build();
        WeComGetFieldsResponse response = weComSmartSheetClient.getFields(accessToken, request);
        assertWeComSuccess(response, "查询字段");
        return response;
    }

    @Override
    public WeComGetRecordsResponse getRecords(String accessToken, String docId, String sheetId,
                                                Integer offset, Integer limit) {
        WeComGetRecordsRequest request = WeComGetRecordsRequest.builder()
                .docid(docId)
                .sheetId(sheetId)
                .keyType(KEY_TYPE_FIELD_ID)
                .offset(offset)
                .limit(limit)
                .build();
        WeComGetRecordsResponse response = weComSmartSheetClient.getRecords(accessToken, request);
        assertWeComSuccess(response, "查询记录");
        return response;
    }

    @Override
    public WeComAddRecordsResponse addRecords(String accessToken, String docId, String sheetId,
                                                List<WeComRecordWriteItem> records) {
        WeComAddRecordsRequest request = WeComAddRecordsRequest.builder()
                .docid(docId)
                .sheetId(sheetId)
                .keyType(KEY_TYPE_FIELD_ID)
                .records(records)
                .build();
        WeComAddRecordsResponse response = weComSmartSheetClient.addRecords(accessToken, request);
        assertWeComSuccess(response, "添加记录");
        return response;
    }

    @Override
    public WeComUpdateRecordsResponse updateRecords(String accessToken, String docId, String sheetId,
                                                      List<WeComRecordWriteItem> records) {
        WeComUpdateRecordsRequest request = WeComUpdateRecordsRequest.builder()
                .docid(docId)
                .sheetId(sheetId)
                .keyType(KEY_TYPE_FIELD_ID)
                .records(records)
                .build();
        WeComUpdateRecordsResponse response = weComSmartSheetClient.updateRecords(accessToken, request);
        assertWeComSuccess(response, "更新记录");
        return response;
    }

    private void assertWeComSuccess(WeComBaseResponse response, String action) {
        if (!response.isSuccess()) {
            log.error("【WeComSmartSheet】{}失败, errcode={}, errmsg={}",
                    action, response.getErrCode(), response.getErrMsg());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    String.format("企业微信%s失败, errcode=%d, errmsg=%s",
                            action, response.getErrCode(), response.getErrMsg()));
        }
    }
}
