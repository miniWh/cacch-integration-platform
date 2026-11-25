package com.cacch.integration.service.heart.api;

import com.cacch.integration.common.exception.BizException;

/**
 * Heart 客户 PO 港口解析服务接口
 *
 * <p>以 PO 上的港口名称（启运港 / 目的港）为关键字，调用勤策自定义对象
 * {@code object_yEQGF__c} 查询对应港口对象 ID，并回写
 * {@code t_integration_crm_heart_po.port_of_loading_id / port_of_discharge_id}；
 * 已存在对象 ID 的港口不再重复查询，保证重试幂等。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IHeartPortService {

    /**
     * 解析并回写指定 PO 的启运港、目的港对象 ID
     *
     * @param poId PO 主键（t_integration_crm_heart_po.id）
     * @throws BizException poId 为空（PARAM_MISSING）、PO 不存在（PARAM_INVALID）、
     *                      港口名称为空（PARAM_MISSING）、未匹配或匹配不唯一（INTEGRATION_ERROR）、
     *                      回写失败（SYSTEM_ERROR）
     */
    void resolveAndSavePorts(Long poId);

    /**
     * 按港口名称查询勤策港口对象 ID
     *
     * @param portName 港口名称关键字（来自 PO 的启运港 / 目的港列）
     * @param portType 港口类型标识（启运港 / 目的港），仅用于日志与异常提示
     * @return 港口对象 ID
     * @throws BizException 港口名称为空（PARAM_MISSING）、未匹配或匹配不唯一（INTEGRATION_ERROR）
     */
    String resolvePortObjectId(String portName, String portType);
}
