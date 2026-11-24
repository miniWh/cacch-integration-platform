package com.cacch.integration.service.crm.api.impl;

import com.cacch.integration.common.constant.crm.CrmContractLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.crm.client.CrmContractClient;
import com.cacch.integration.integration.crm.client.dto.CrmContractAddRequest;
import com.cacch.integration.integration.crm.client.dto.CrmContractAddResponse;
import com.cacch.integration.integration.crm.client.dto.CrmContractDetailItem;
import com.cacch.integration.service.crm.api.ICrmContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

/**
 * 勤策合同服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmContractServiceImpl implements ICrmContractService {

    private final CrmContractClient crmContractClient;

    @Override
    public CrmContractAddResponse addContract(CrmContractAddRequest request) {
        validate(request);
        log.info("【{}】新增销售合同, customerId={}, customerCode={}, detailSize={}",
                CrmContractLogConstants.LOG_BIZ, request.getCustomerId(), request.getCustomerCode(),
                request.getDetailList().size());
        try {
            CrmContractAddResponse response = crmContractClient.addContract(request);
            if (!response.isSuccess()) {
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "新增销售合同失败, return_code=" + response.getReturnCode()
                                + ", return_msg=" + response.getReturnMsg());
            }
            return response;
        } catch (BizException e) {
            log.error("【{}】新增销售合同终止, reason={}", CrmContractLogConstants.LOG_BIZ, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "新增销售合同调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 入参基础校验，与勤策文档必填约束保持一致
     *
     * @param request 新增销售合同请求体
     */
    private void validate(CrmContractAddRequest request) {
        if (request == null) {
            log.info("【{}】新增销售合同终止, reason=请求体为空", CrmContractLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "新增销售合同请求体不能为空");
        }
        if (request.getCustomerId() == null && !StringUtils.hasText(request.getCustomerCode())) {
            log.info("【{}】新增销售合同终止, reason=客户标识为空", CrmContractLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "customerId 与 customerCode 至少传一个");
        }
        if (Boolean.TRUE.equals(request.getIsTrigger()) && request.getTriggerEmployee() == null) {
            log.info("【{}】新增销售合同终止, reason=isTrigger为true但triggerEmployee为空",
                    CrmContractLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "isTrigger 为 true 时 triggerEmployee 不能为空");
        }
        List<CrmContractDetailItem> details = request.getDetailList();
        if (CollectionUtils.isEmpty(details)) {
            log.info("【{}】新增销售合同终止, reason=明细为空", CrmContractLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "detailList 不能为空");
        }
        for (int i = 0; i < details.size(); i++) {
            CrmContractDetailItem detail = details.get(i);
            int no = i + 1;
            if (detail == null) {
                log.info("【{}】新增销售合同终止, reason=第{}条明细为空", CrmContractLogConstants.LOG_BIZ, no);
                throw new BizException(ResultCode.PARAM_MISSING, "第 " + no + " 条明细不能为空");
            }
            if (detail.getPdId() == null && !StringUtils.hasText(detail.getPdCode())
                    && !StringUtils.hasText(detail.getPdSourceCode())) {
                log.info("【{}】新增销售合同终止, reason=第{}条明细商品标识为空", CrmContractLogConstants.LOG_BIZ, no);
                throw new BizException(ResultCode.PARAM_MISSING,
                        "第 " + no + " 条明细 pdId、pdCode、pdSourceCode 至少传一个");
            }
            if (detail.getPdCount() == null || detail.getPdCount().compareTo(BigDecimal.ZERO) <= 0) {
                log.info("【{}】新增销售合同终止, reason=第{}条明细数量非法", CrmContractLogConstants.LOG_BIZ, no);
                throw new BizException(ResultCode.PARAM_MISSING, "第 " + no + " 条明细 pdCount 必须大于 0");
            }
            if (detail.getActualPrice() == null || detail.getActualPrice().compareTo(BigDecimal.ZERO) < 0) {
                log.info("【{}】新增销售合同终止, reason=第{}条明细售价非法", CrmContractLogConstants.LOG_BIZ, no);
                throw new BizException(ResultCode.PARAM_MISSING, "第 " + no + " 条明细 actualPrice 必须大于等于 0");
            }
        }
    }
}
