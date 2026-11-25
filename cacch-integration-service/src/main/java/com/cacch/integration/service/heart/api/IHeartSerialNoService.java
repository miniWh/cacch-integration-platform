package com.cacch.integration.service.heart.api;

import com.cacch.integration.common.exception.BizException;

/**
 * Heart 客户 PO 取号服务接口
 *
 * <p>调用勤策自定义对象接口为指定 PO 取 CAC PO 流水号，并回写
 * {@code t_integration_heart_po.cac_po_no}；同一 PO 仅取号一次，失败重试复用已取号码。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IHeartSerialNoService {

    /**
     * 为指定 PO 获取 CAC PO 流水号
     *
     * <p>PO 已存在 CAC PO 号时直接复用，不再重复取号。</p>
     *
     * @param poId PO 主键（t_integration_heart_po.id）
     * @return CAC PO 流水号
     * @throws BizException poId 为空（PARAM_MISSING）、PO 不存在（PARAM_INVALID）、
     *                      勤策取号失败（INTEGRATION_ERROR）或回写失败（SYSTEM_ERROR）
     */
    String acquireCacPoNo(Long poId);
}
