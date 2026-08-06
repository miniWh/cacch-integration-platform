package com.cacch.integration.manager.fdd.api.impl;

import com.cacch.integration.common.config.fdd.FddProperties;
import com.cacch.integration.common.constant.fdd.FddConstants;
import com.cacch.integration.common.enums.fdd.FddAuthStatusEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.fdd.FddEnterpriseAuthDO;
import com.cacch.integration.integration.fdd.client.FddClient;
import com.cacch.integration.integration.fdd.client.dto.FddCallbackRequest;
import com.cacch.integration.service.fdd.api.IFddEnterpriseAuthService;
import com.cacch.integration.service.fdd.api.IFddPersonAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 企业认证回调单测（基于历史回调报文验证处理链路，不改动正式流程）
 *
 * @author hongfu_zhou@cacch.com
 */
@ExtendWith(MockitoExtension.class)
class FddAuthManagerImplEnterpriseCallbackTest {

    private static final String TRANSACTION_NO = "cdf0548ae12b48c58e7559cd5b445ab2";
    private static final String COMPANY_ID = "2052226862729527297";

    @Mock
    private IFddEnterpriseAuthService fddEnterpriseAuthService;
    @Mock
    private IFddPersonAuthService fddPersonAuthService;
    @Mock
    private FddClient fddClient;
    @Mock
    private FddProperties fddProperties;

    private FddAuthManagerImpl manager;

    @BeforeEach
    void setUp() {
        manager = new FddAuthManagerImpl(
                fddEnterpriseAuthService, fddPersonAuthService, fddClient, fddProperties);
    }

    @Test
    void handleEnterpriseCallback_pendingToSuccess_withHistoryPayload() {
        FddEnterpriseAuthDO pending = pendingRecord();
        when(fddEnterpriseAuthService.findByTransactionNo(TRANSACTION_NO)).thenReturn(pending);

        FddCallbackRequest request = historyCallbackRequest();
        Map<String, Object> raw = historyRawPayload();

        assertDoesNotThrow(() -> manager.handleEnterpriseCallback(request, raw));

        verify(fddEnterpriseAuthService).updateByCallback(
                eq(pending.getId()),
                eq(FddAuthStatusEnum.SUCCESS.getCode()),
                eq(raw),
                isNull(),
                eq(COMPANY_ID),
                isNull());
    }

    @Test
    void handleEnterpriseCallback_alreadySuccess_idempotentSkip() {
        FddEnterpriseAuthDO success = pendingRecord();
        success.setAuthStatus(FddAuthStatusEnum.SUCCESS.getCode());
        when(fddEnterpriseAuthService.findByTransactionNo(TRANSACTION_NO)).thenReturn(success);

        assertDoesNotThrow(() -> manager.handleEnterpriseCallback(historyCallbackRequest(), historyRawPayload()));

        verify(fddEnterpriseAuthService, never()).updateByCallback(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void handleEnterpriseCallback_transactionNotFound_throws() {
        when(fddEnterpriseAuthService.findByTransactionNo(TRANSACTION_NO)).thenReturn(null);

        assertThrows(BizException.class,
                () -> manager.handleEnterpriseCallback(historyCallbackRequest(), historyRawPayload()));

        verify(fddEnterpriseAuthService, never()).updateByCallback(
                any(), any(), any(), any(), any(), any());
    }

    private static FddEnterpriseAuthDO pendingRecord() {
        FddEnterpriseAuthDO record = new FddEnterpriseAuthDO();
        record.setId(1001L);
        record.setInternalCompanyName("上海泰禾国际贸易有限公司");
        record.setEnterpriseName("江苏稼穑化学有限公司");
        record.setUscc("91320826692585328W");
        record.setTransactionNo(TRANSACTION_NO);
        record.setFddCompanyId(COMPANY_ID);
        record.setAuthStatus(FddAuthStatusEnum.PENDING.getCode());
        return record;
    }

    private static FddCallbackRequest historyCallbackRequest() {
        FddCallbackRequest request = new FddCallbackRequest();
        request.setNotifyType(FddConstants.NOTIFY_TYPE_ENTERPRISE);
        request.setCompanyId(COMPANY_ID);
        request.setTpOrgId(null);
        request.setStatus(FddConstants.ENTERPRISE_STATUS_CERTIFIED);
        request.setTransactionNo(TRANSACTION_NO);
        return request;
    }

    private static Map<String, Object> historyRawPayload() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("notifyType", FddConstants.NOTIFY_TYPE_ENTERPRISE);
        raw.put("companyId", COMPANY_ID);
        raw.put("tpOrgId", null);
        raw.put("status", FddConstants.ENTERPRISE_STATUS_CERTIFIED);
        raw.put("transactionNo", TRANSACTION_NO);
        return raw;
    }
}
