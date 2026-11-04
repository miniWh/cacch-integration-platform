package com.cacch.integration.service.keruyun.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderItemDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderPaymentDO;
import com.cacch.integration.mapper.keruyun.KeruyunOrderItemMapper;
import com.cacch.integration.mapper.keruyun.KeruyunOrderMapper;
import com.cacch.integration.mapper.keruyun.KeruyunOrderPaymentMapper;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客如云订单详情持久化服务实现
 *
 * <p>幂等策略：单订单明细整体替换（delete + insert），主表 detail_synced_at
 * 更新与明细写入在同一事务，失败整体回滚，重跑不产生重复数据。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeruyunOrderDetailServiceImpl implements IKeruyunOrderDetailService {

    private static final String BIZ = "客如云订单详情";

    private final KeruyunOrderMapper keruyunOrderMapper;
    private final KeruyunOrderItemMapper keruyunOrderItemMapper;
    private final KeruyunOrderPaymentMapper keruyunOrderPaymentMapper;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 15, rollbackFor = Exception.class)
    public List<KeruyunOrderDO> listOrdersPendingDetail(int limit) {
        if (limit <= 0) {
            log.info("【{}】待同步订单查询终止, reason=limit非法, limit={}", BIZ, limit);
            throw new BizException(ResultCode.PARAM_INVALID, "limit 必须大于 0");
        }
        return keruyunOrderMapper.selectList(
                new LambdaQueryWrapper<KeruyunOrderDO>()
                        .isNotNull(KeruyunOrderDO::getFinishTime)
                        .isNull(KeruyunOrderDO::getDetailSyncedAt)
                        .orderByAsc(KeruyunOrderDO::getFinishTime)
                        .last("LIMIT " + limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void saveOrderDetails(String orderId, List<KeruyunOrderItemDO> items, List<KeruyunOrderPaymentDO> payments) {
        if (!StringUtils.hasText(orderId)) {
            log.info("【{}】明细写入终止, reason=订单ID为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "客如云订单 orderId 不能为空");
        }

        // 整体替换：先清理旧明细，保证重跑幂等
        keruyunOrderItemMapper.delete(new LambdaQueryWrapper<KeruyunOrderItemDO>()
                .eq(KeruyunOrderItemDO::getOrderId, orderId));
        keruyunOrderPaymentMapper.delete(new LambdaQueryWrapper<KeruyunOrderPaymentDO>()
                .eq(KeruyunOrderPaymentDO::getOrderId, orderId));

        if (items != null && !items.isEmpty()) {
            items.forEach(keruyunOrderItemMapper::insert);
        }
        if (payments != null && !payments.isEmpty()) {
            payments.forEach(keruyunOrderPaymentMapper::insert);
        }

        // 主表标记详情同步成功时间，作为待同步队列的出队依据
        keruyunOrderMapper.update(null, new LambdaUpdateWrapper<KeruyunOrderDO>()
                .eq(KeruyunOrderDO::getOrderId, orderId)
                .set(KeruyunOrderDO::getDetailSyncedAt, LocalDateTime.now()));
    }
}
