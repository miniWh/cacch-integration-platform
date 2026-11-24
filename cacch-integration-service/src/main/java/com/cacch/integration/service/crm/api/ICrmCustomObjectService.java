package com.cacch.integration.service.crm.api;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryResponse;

/**
 * 勤策自定义对象服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmCustomObjectService {

    /**
     * 查询自定义对象数据
     *
     * <p>object_key 为空时直接拒绝；page / rows 未指定时分别为 1 / 100；
     * sorts 未指定时默认按 create_time 倒序。</p>
     *
     * @param request 查询条件，可为空；为空时因缺少 object_key 抛出参数异常
     * @return 自定义对象数据响应，response_data 为动态字段对象列表
     * @throws BizException object_key 为空（PARAM_MISSING）或勤策返回失败码（INTEGRATION_ERROR）
     */
    CrmCustomObjectQueryResponse queryObjectData(CrmCustomObjectQueryRequest request);
}
