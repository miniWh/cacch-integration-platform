package com.cacch.integration.service.keruyun.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import com.cacch.integration.mapper.keruyun.KeruyunOrderMapper;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 客如云订单持久化服务实现
 *
 * <p>以 order_id 为业务唯一键：先按唯一键查询，命中则 updateById，未命中则 insert。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeruyunOrderServiceImpl implements IKeruyunOrderService {

    private static final String BIZ = "客如云订单";

    private final KeruyunOrderMapper keruyunOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public boolean saveOrUpdate(KeruyunOrderDO orderDO) {
        if (orderDO == null || !StringUtils.hasText(orderDO.getOrderId())) {
            log.info("【{}】写入终止, reason=订单ID为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "客如云订单 orderId 不能为空");
        }

        KeruyunOrderDO existing = keruyunOrderMapper.selectOne(
                new LambdaQueryWrapper<KeruyunOrderDO>()
                        .eq(KeruyunOrderDO::getOrderId, orderDO.getOrderId())
                        .last("LIMIT 1"));
        if (existing == null) {
            keruyunOrderMapper.insert(orderDO);
            return true;
        }
        orderDO.setId(existing.getId());
        keruyunOrderMapper.updateById(orderDO);
        return false;
    }
}
