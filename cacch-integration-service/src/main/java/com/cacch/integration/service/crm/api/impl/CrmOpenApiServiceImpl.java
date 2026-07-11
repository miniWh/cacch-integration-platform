package com.cacch.integration.service.crm.api.impl;

import com.cacch.integration.common.constant.crm.CrmConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.crm.adapter.CrmQueryRequestFactory;
import com.cacch.integration.integration.crm.client.CrmClient;
import com.cacch.integration.integration.crm.client.dto.CrmEmployeeQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.crm.client.dto.CrmPageQueryRequest;
import com.cacch.integration.integration.crm.support.CrmTimeSupport;
import com.cacch.integration.service.crm.api.ICrmOpenApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

/**
 * 勤策 CRM OpenAPI 服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmOpenApiServiceImpl implements ICrmOpenApiService {

    private final CrmClient crmClient;

    @Override
    public CrmOpenApiResponse orderQuery(CrmPageQueryRequest request) {
        if (request == null) {
            log.info("【CrmOpenApi】查询订单终止, reason=请求体为空");
            throw new BizException(ResultCode.PARAM_MISSING, "订单查询请求体不能为空");
        }
        return invoke(() -> crmClient.orderQuery(request), "查询订单");
    }

    @Override
    public CrmOpenApiResponse orderQueryByModifyTime(Integer page, Integer rows, String beginTime, String endTime) {
        if (!StringUtils.hasText(beginTime)) {
            log.info("【CrmOpenApi】按修改时间查订单终止, reason=beginTime为空");
            throw new BizException(ResultCode.PARAM_MISSING, "beginTime 不能为空");
        }
        int p = page == null || page < 1 ? CrmConstants.DEFAULT_PAGE : page;
        int r = rows == null || rows < 1 ? CrmConstants.DEFAULT_ROWS : rows;
        String beginMilli = CrmTimeSupport.toEpochMilliString(beginTime, "beginTime");
        String endMilli = StringUtils.hasText(endTime)
                ? CrmTimeSupport.toEpochMilliString(endTime, "endTime")
                : null;
        log.info("【CrmOpenApi】按修改时间查订单, beginTime={}, beginMilli={}, endTime={}, endMilli={}",
                beginTime, beginMilli, endTime, endMilli);
        CrmPageQueryRequest request = CrmQueryRequestFactory.orderByModifyTime(p, r, beginMilli, endMilli);
        return orderQuery(request);
    }

    @Override
    public CrmOpenApiResponse orderDetailQuery(CrmPageQueryRequest request) {
        if (request == null) {
            log.info("【CrmOpenApi】查询订单明细终止, reason=请求体为空");
            throw new BizException(ResultCode.PARAM_MISSING, "订单明细查询请求体不能为空");
        }
        return invoke(() -> crmClient.orderDetailQuery(request), "查询订单明细");
    }

    @Override
    public CrmOpenApiResponse orderDetailQueryByOrderId(Integer page, Integer rows, String crmOrderId) {
        if (!StringUtils.hasText(crmOrderId)) {
            log.info("【CrmOpenApi】按订单ID查明细终止, reason=crmOrderId为空");
            throw new BizException(ResultCode.PARAM_MISSING, "crmOrderId 不能为空");
        }
        int p = page == null || page < 1 ? CrmConstants.DEFAULT_PAGE : page;
        int r = rows == null || rows < 1 ? CrmConstants.DEFAULT_ROWS : rows;
        CrmPageQueryRequest request = CrmQueryRequestFactory.detailByOrderId(p, r, crmOrderId.trim());
        return orderDetailQuery(request);
    }

    @Override
    public CrmOpenApiResponse queryEmployee(CrmEmployeeQueryRequest request) {
        if (request == null) {
            log.info("【CrmOpenApi】查询员工终止, reason=请求体为空");
            throw new BizException(ResultCode.PARAM_MISSING, "员工查询请求体不能为空");
        }
        return invoke(() -> crmClient.queryEmployee(request), "查询员工帐号");
    }

    @Override
    public CrmOpenApiResponse queryEmployeeById(String employeeId) {
        if (!StringUtils.hasText(employeeId)) {
            log.info("【CrmOpenApi】按员工ID查询终止, reason=employeeId为空");
            throw new BizException(ResultCode.PARAM_MISSING, "employeeId 不能为空");
        }
        CrmEmployeeQueryRequest request = CrmEmployeeQueryRequest.builder()
                .id(employeeId.trim())
                .build();
        return queryEmployee(request);
    }

    private CrmOpenApiResponse invoke(CrmCall call, String action) {
        try {
            CrmOpenApiResponse response = call.execute();
            if (!response.isSuccess()) {
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        action + "失败, return_code=" + response.getReturnCode()
                                + ", return_msg=" + response.getReturnMsg());
            }
            return response;
        } catch (BizException e) {
            log.error("【CrmOpenApi】{}终止, reason={}", action, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, action + "调用失败: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface CrmCall {
        CrmOpenApiResponse execute();
    }
}
