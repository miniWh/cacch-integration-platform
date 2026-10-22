package com.cacch.integration.service.keruyun.api;

import com.cacch.integration.entity.keruyun.KeruyunOrderDO;

/**
 * 客如云订单持久化服务接口
 *
 * <p>单聚合（订单快照）内的业务逻辑：以 order_id 为业务唯一键执行幂等写入，
 * 不做跨聚合编排与第三方调用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunOrderService {

    /**
     * 按 order_id 幂等写入订单（存在则更新，不存在则新增）
     *
     * @param orderDO 待写入订单，orderId 不可为空；新增时忽略传入的 id
     * @return true 表示新增，false 表示更新
     * @throws com.cacch.integration.common.exception.BizException orderId 为空时抛出
     */
    boolean saveOrUpdate(KeruyunOrderDO orderDO);
}
