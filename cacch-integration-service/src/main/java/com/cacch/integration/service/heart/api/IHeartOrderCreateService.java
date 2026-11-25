package com.cacch.integration.service.heart.api;

import com.cacch.integration.common.dto.heart.HeartOrderCreateResult;
import com.cacch.integration.common.exception.BizException;

/**
 * Heart 客户 PO 建单服务接口
 *
 * <p>以 PO 主表 + 明细行为数据源组装勤策 {@code contractAdd} 报文，
 * 完成幂等校验、请求提交、提交记录落库与流程状态推进。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IHeartOrderCreateService {

    /**
     * 将指定 PO 提交勤策生成销售合同
     *
     * <p>幂等规则：已存在 {@code SUCCESS} 提交记录时直接返回已有合同号，不重复建单；
     * CRM 调用失败时落 {@code FAILED} 记录并回写失败原因，不抛出异常。</p>
     *
     * @param poId PO 主键（t_integration_crm_heart_po.id）
     * @return 建单结果
     * @throws BizException poId 为空（PARAM_MISSING）、PO 不存在（PARAM_INVALID）、
     *                      PO 状态非 PORT_RESOLVED（PARAM_INVALID）
     */
    HeartOrderCreateResult createOrder(Long poId);
}
