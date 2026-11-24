package com.cacch.integration.service.crm.api.impl;

import com.cacch.integration.common.constant.crm.CrmCustomObjectConstants;
import com.cacch.integration.common.constant.crm.CrmCustomObjectLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.crm.client.CrmCustomObjectClient;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryResponse;
import com.cacch.integration.integration.crm.client.dto.CrmSortItem;
import com.cacch.integration.service.crm.api.ICrmCustomObjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * 勤策自定义对象服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmCustomObjectServiceImpl implements ICrmCustomObjectService {

    private final CrmCustomObjectClient crmCustomObjectClient;

    @Override
    public CrmCustomObjectQueryResponse queryObjectData(CrmCustomObjectQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.getObjectKey())) {
            log.info("【{}】查询自定义对象数据终止, reason=object_key为空", CrmCustomObjectLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "object_key 不能为空");
        }
        request.setObjectKey(request.getObjectKey().trim());
        if (request.getPage() == null || request.getPage() < 1) {
            request.setPage(CrmCustomObjectConstants.DEFAULT_PAGE_NUMBER);
        }
        if (request.getRows() == null || request.getRows() < 1) {
            request.setRows(CrmCustomObjectConstants.DEFAULT_PAGE_SIZE);
        }
        if (request.getSorts() == null || request.getSorts().isEmpty()) {
            request.setSorts(List.of(CrmSortItem.builder()
                    .fieldKey(CrmCustomObjectConstants.DEFAULT_SORT_FIELD)
                    .type(CrmCustomObjectConstants.SORT_TYPE_DESC)
                    .build()));
        }
        log.info("【{}】查询自定义对象数据, objectKey={}, page={}, rows={}",
                CrmCustomObjectLogConstants.LOG_BIZ, request.getObjectKey(), request.getPage(), request.getRows());
        try {
            CrmCustomObjectQueryResponse response = crmCustomObjectClient.queryObjectData(request);
            if (!response.isSuccess()) {
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "查询自定义对象数据失败, return_code=" + response.getReturnCode()
                                + ", return_msg=" + response.getReturnMsg());
            }
            return response;
        } catch (BizException e) {
            log.error("【{}】查询自定义对象数据终止, reason={}", CrmCustomObjectLogConstants.LOG_BIZ, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "查询自定义对象数据调用失败: " + e.getMessage(), e);
        }
    }
}
