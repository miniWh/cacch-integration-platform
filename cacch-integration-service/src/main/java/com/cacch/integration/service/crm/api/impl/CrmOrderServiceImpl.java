package com.cacch.integration.service.crm.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cacch.integration.common.enums.crm.CrmDetailFetchStatusEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.crm.CrmOrderDO;
import com.cacch.integration.mapper.crm.CrmOrderMapper;
import com.cacch.integration.service.crm.api.ICrmOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * CRM 订单持久化服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmOrderServiceImpl implements ICrmOrderService {

    private final CrmOrderMapper crmOrderMapper;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public CrmOrderDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return crmOrderMapper.selectById(id);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public CrmOrderDO getByOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        return crmOrderMapper.selectOne(new LambdaQueryWrapper<CrmOrderDO>()
                .eq(CrmOrderDO::getOrderNo, orderNo.trim())
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public CrmOrderDO getByCrmOrderId(String crmOrderId) {
        if (!StringUtils.hasText(crmOrderId)) {
            return null;
        }
        return crmOrderMapper.selectOne(new LambdaQueryWrapper<CrmOrderDO>()
                .eq(CrmOrderDO::getCrmOrderId, crmOrderId.trim())
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public CrmOrderDO insert(CrmOrderDO order) {
        if (order == null || !StringUtils.hasText(order.getOrderNo()) || !StringUtils.hasText(order.getCrmOrderId())) {
            log.info("【CrmOrder】新增终止, reason=订单编号或CRM订单ID为空");
            throw new BizException(ResultCode.PARAM_MISSING, "订单编号或 CRM 订单 ID 不能为空");
        }
        if (!StringUtils.hasText(order.getDetailFetchStatus())) {
            order.setDetailFetchStatus(CrmDetailFetchStatusEnum.PENDING.getCode());
        }
        crmOrderMapper.insert(order);
        log.info("【CrmOrder】新增订单, orderNo={}, crmOrderId={}, id={}",
                order.getOrderNo(), order.getCrmOrderId(), order.getId());
        return order;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public void updateDetailFetchStatus(Long orderId, String status, String error) {
        if (orderId == null || !StringUtils.hasText(status)) {
            log.info("【CrmOrder】更新明细拉取状态终止, reason=参数无效, orderId={}", orderId);
            return;
        }
        crmOrderMapper.update(null, new LambdaUpdateWrapper<CrmOrderDO>()
                .eq(CrmOrderDO::getId, orderId)
                .set(CrmOrderDO::getDetailFetchStatus, status)
                .set(CrmOrderDO::getDetailFetchError, error)
                .set(CrmOrderDO::getDetailFetchTime, LocalDateTime.now()));
        log.info("【CrmOrder】更新明细拉取状态, orderId={}, status={}", orderId, status);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public List<CrmOrderDO> listNeedDetailRetry(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        return crmOrderMapper.selectList(new LambdaQueryWrapper<CrmOrderDO>()
                .in(CrmOrderDO::getDetailFetchStatus,
                        CrmDetailFetchStatusEnum.PENDING.getCode(),
                        CrmDetailFetchStatusEnum.FAILED.getCode())
                .orderByAsc(CrmOrderDO::getId)
                .last("LIMIT " + limit));
    }
}
