package com.cacch.integration.service.keruyun.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客如云订单持久化服务实现
 *
 * <p>以 order_id 为业务唯一键：先按唯一键查询，命中则 updateById，未命中则 insert；
 * 查询方法基于 MyBatis-Plus LambdaQueryWrapper 动态拼装条件。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeruyunOrderServiceImpl implements IKeruyunOrderService {

    private static final String BIZ = "客如云订单";
    private static final int MAX_PAGE_SIZE = 100;

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

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 15, rollbackFor = Exception.class)
    public KeruyunOrderDO getByOrderId(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            log.info("【{}】按orderId查询终止, reason=orderId为空", BIZ);
            return null;
        }
        return keruyunOrderMapper.selectOne(
                new LambdaQueryWrapper<KeruyunOrderDO>()
                        .eq(KeruyunOrderDO::getOrderId, orderId)
                        .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 15, rollbackFor = Exception.class)
    public IPage<KeruyunOrderDO> queryByCondition(String orderId, String shopId, String orderStatus, String orderType,
                                                  LocalDateTime openTimeFrom, LocalDateTime openTimeTo,
                                                  LocalDateTime createdAtFrom, LocalDateTime createdAtTo,
                                                  int pageNum, int pageSize) {
        // 截断 pageSize 防止过大扫描
        int effectivePageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        int effectivePageNum = Math.max(1, pageNum);

        LambdaQueryWrapper<KeruyunOrderDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderId)) {
            wrapper.eq(KeruyunOrderDO::getOrderId, orderId);
        }
        if (StringUtils.hasText(shopId)) {
            wrapper.eq(KeruyunOrderDO::getShopId, shopId);
        }
        if (StringUtils.hasText(orderStatus)) {
            wrapper.eq(KeruyunOrderDO::getOrderStatus, orderStatus);
        }
        if (StringUtils.hasText(orderType)) {
            wrapper.eq(KeruyunOrderDO::getOrderType, orderType);
        }
        if (openTimeFrom != null) {
            wrapper.ge(KeruyunOrderDO::getOpenTime, openTimeFrom);
        }
        if (openTimeTo != null) {
            wrapper.le(KeruyunOrderDO::getOpenTime, openTimeTo);
        }
        if (createdAtFrom != null) {
            wrapper.ge(KeruyunOrderDO::getCreatedAt, createdAtFrom);
        }
        if (createdAtTo != null) {
            wrapper.le(KeruyunOrderDO::getCreatedAt, createdAtTo);
        }
        // 默认按下单时间倒序
        wrapper.orderByDesc(KeruyunOrderDO::getOpenTime);

        return keruyunOrderMapper.selectPage(new Page<>(effectivePageNum, effectivePageSize), wrapper);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 30, rollbackFor = Exception.class)
    public List<KeruyunOrderDO> listForExport(String orderId, String shopId, String orderStatus, String orderType,
                                              LocalDateTime openTimeFrom, LocalDateTime openTimeTo,
                                              LocalDateTime createdAtFrom, LocalDateTime createdAtTo,
                                              int maxRows) {
        if (maxRows <= 0) {
            log.info("【{}】导出查询终止, reason=maxRows非法, maxRows={}", BIZ, maxRows);
            throw new BizException(ResultCode.PARAM_INVALID, "maxRows 必须大于 0");
        }

        LambdaQueryWrapper<KeruyunOrderDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderId)) {
            wrapper.eq(KeruyunOrderDO::getOrderId, orderId);
        }
        if (StringUtils.hasText(shopId)) {
            wrapper.eq(KeruyunOrderDO::getShopId, shopId);
        }
        if (StringUtils.hasText(orderStatus)) {
            wrapper.eq(KeruyunOrderDO::getOrderStatus, orderStatus);
        }
        if (StringUtils.hasText(orderType)) {
            wrapper.eq(KeruyunOrderDO::getOrderType, orderType);
        }
        if (openTimeFrom != null) {
            wrapper.ge(KeruyunOrderDO::getOpenTime, openTimeFrom);
        }
        if (openTimeTo != null) {
            wrapper.le(KeruyunOrderDO::getOpenTime, openTimeTo);
        }
        if (createdAtFrom != null) {
            wrapper.ge(KeruyunOrderDO::getCreatedAt, createdAtFrom);
        }
        if (createdAtTo != null) {
            wrapper.le(KeruyunOrderDO::getCreatedAt, createdAtTo);
        }
        // 导出场景按 open_time 升序；多取 1 行用于判断是否超过 maxRows
        wrapper.orderByAsc(KeruyunOrderDO::getOpenTime);
        wrapper.last("LIMIT " + (maxRows + 1));

        List<KeruyunOrderDO> rows = keruyunOrderMapper.selectList(wrapper);
        if (rows.size() > maxRows) {
            log.info("【{}】导出查询终止, reason=结果超出上限, maxRows={}, actual>{}", BIZ, maxRows, maxRows);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "导出数据量超过上限 " + maxRows + " 条，请缩小查询条件范围");
        }
        return rows;
    }
}
