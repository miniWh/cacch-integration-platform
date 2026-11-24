package com.cacch.integration.service.crm.api;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryResponse;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectSaveRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectSaveResponse;

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

    /**
     * 创建自定义对象数据
     *
     * <p>object_key 必填；is_trigger=true 时 trigger_employee 必填。
     * fields、slave_fields 由 Client 序列化为 JSON 字符串后出站。</p>
     *
     * @param request 创建请求
     * @return 创建响应，response_data 为新建数据标识节点（含 id）
     * @throws BizException 必填校验不通过（PARAM_MISSING）或勤策返回失败码（INTEGRATION_ERROR）
     */
    CrmCustomObjectSaveResponse saveObjectData(CrmCustomObjectSaveRequest request);
}
