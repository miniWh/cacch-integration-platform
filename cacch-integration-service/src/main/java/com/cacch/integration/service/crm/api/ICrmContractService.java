package com.cacch.integration.service.crm.api;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.integration.crm.client.dto.CrmContractAddRequest;
import com.cacch.integration.integration.crm.client.dto.CrmContractAddResponse;

/**
 * 勤策合同服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmContractService {

    /**
     * 新增销售合同
     *
     * <p>调用前做基础校验：客户标识（customerId / customerCode）至少一个、
     * 明细非空且每条明细商品标识至少一个、数量大于 0、实际售价大于等于 0；
     * isTrigger=true 时 triggerEmployee 必填。</p>
     *
     * @param request 新增销售合同请求体，入参 camelCase
     * @return 新增销售合同响应，response_data 为 JSON 节点（含新增合同 ID）
     * @throws BizException 必填校验不通过（PARAM_MISSING）或勤策返回失败码（INTEGRATION_ERROR）
     */
    CrmContractAddResponse addContract(CrmContractAddRequest request);
}
