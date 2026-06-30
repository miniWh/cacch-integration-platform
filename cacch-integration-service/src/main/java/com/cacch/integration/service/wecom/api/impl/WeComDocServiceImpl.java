package com.cacch.integration.service.wecom.api.impl;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.WeComDocClient;
import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocRequest;
import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.cacch.integration.service.wecom.api.IWeComDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeComDocServiceImpl implements IWeComDocService {

    private final WeComDocClient weComDocClient;

    @Override
    public WeComCreateDocResponse createSmartSheetDoc(String accessToken, String docName, List<String> adminUsers) {
        WeComCreateDocRequest request = WeComCreateDocRequest.builder()
                .docType(WeComConstants.DOC_TYPE_SMART_SHEET)
                .docName(docName)
                .adminUsers(adminUsers)
                .build();
        WeComCreateDocResponse response = weComDocClient.createDoc(accessToken, request);
        assertWeComSuccess(response, "新建智能表格");
        return response;
    }

    private void assertWeComSuccess(WeComBaseResponse response, String action) {
        if (!response.isSuccess()) {
            log.error("【WeComDoc】{}失败, errcode={}, errmsg={}",
                    action, response.getErrCode(), response.getErrMsg());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    String.format("企业微信%s失败, errcode=%d, errmsg=%s",
                            action, response.getErrCode(), response.getErrMsg()));
        }
    }
}
