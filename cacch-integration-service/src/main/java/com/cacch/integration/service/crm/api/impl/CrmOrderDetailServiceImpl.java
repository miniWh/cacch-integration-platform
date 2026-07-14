package com.cacch.integration.service.crm.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cacch.integration.common.constant.crm.CrmOaFormConstants;
import com.cacch.integration.common.enums.crm.CrmOaSyncStatusEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.crm.CrmOrderDetailDO;
import com.cacch.integration.mapper.crm.CrmOrderDetailMapper;
import com.cacch.integration.service.crm.api.ICrmOrderDetailService;
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
 * CRM 订单明细持久化服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmOrderDetailServiceImpl implements ICrmOrderDetailService {

    private final CrmOrderDetailMapper crmOrderDetailMapper;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public boolean existsByCrmDetailId(String crmDetailId) {
        if (!StringUtils.hasText(crmDetailId)) {
            return false;
        }
        Long count = crmOrderDetailMapper.selectCount(new LambdaQueryWrapper<CrmOrderDetailDO>()
                .eq(CrmOrderDetailDO::getCrmDetailId, crmDetailId.trim()));
        return count != null && count > 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public CrmOrderDetailDO insert(CrmOrderDetailDO detail) {
        if (detail == null || !StringUtils.hasText(detail.getCrmDetailId()) || detail.getOrderId() == null) {
            log.info("【CrmOrderDetail】新增终止, reason=明细ID或orderId为空");
            throw new BizException(ResultCode.PARAM_MISSING, "明细 ID 或 orderId 不能为空");
        }
        if (!StringUtils.hasText(detail.getOaSyncStatus())) {
            detail.setOaSyncStatus(CrmOaSyncStatusEnum.PENDING.getCode());
        }
        if (detail.getRetryCount() == null) {
            detail.setRetryCount(0);
        }
        crmOrderDetailMapper.insert(detail);
        log.info("【CrmOrderDetail】新增明细, crmDetailId={}, orderId={}, id={}",
                detail.getCrmDetailId(), detail.getOrderId(), detail.getId());
        return detail;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public List<CrmOrderDetailDO> listPendingOrRetry(int limit, int maxRetry) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        int resolvedMaxRetry = maxRetry > 0 ? maxRetry : CrmOaFormConstants.DEFAULT_MAX_RETRY;
        return crmOrderDetailMapper.selectList(new LambdaQueryWrapper<CrmOrderDetailDO>()
                .in(CrmOrderDetailDO::getOaSyncStatus,
                        CrmOaSyncStatusEnum.PENDING.getCode(),
                        CrmOaSyncStatusEnum.RETRY.getCode())
                .lt(CrmOrderDetailDO::getRetryCount, resolvedMaxRetry)
                .orderByAsc(CrmOrderDetailDO::getId)
                .last("LIMIT " + limit));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public void markOaSyncSuccess(Long detailId, String oaProcessId) {
        if (detailId == null) {
            log.info("【CrmOrderDetail】OA成功回写终止, reason=detailId为空");
            return;
        }
        crmOrderDetailMapper.update(null, new LambdaUpdateWrapper<CrmOrderDetailDO>()
                .eq(CrmOrderDetailDO::getId, detailId)
                .set(CrmOrderDetailDO::getOaSyncStatus, CrmOaSyncStatusEnum.SUCCESS.getCode())
                .set(CrmOrderDetailDO::getOaProcessId, oaProcessId)
                .set(CrmOrderDetailDO::getOaSyncTime, LocalDateTime.now())
                .set(CrmOrderDetailDO::getLastErrorMsg, null));
        log.info("【CrmOrderDetail】OA同步成功, detailId={}, oaProcessId={}", detailId, oaProcessId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public String markOaSyncFailure(Long detailId, String errorMsg, int maxRetry) {
        if (detailId == null) {
            log.info("【CrmOrderDetail】OA失败回写终止, reason=detailId为空");
            return CrmOaSyncStatusEnum.RETRY.getCode();
        }
        CrmOrderDetailDO current = crmOrderDetailMapper.selectById(detailId);
        if (current == null) {
            log.info("【CrmOrderDetail】OA失败回写终止, reason=明细不存在, detailId={}", detailId);
            return CrmOaSyncStatusEnum.RETRY.getCode();
        }
        int resolvedMaxRetry = maxRetry > 0 ? maxRetry : CrmOaFormConstants.DEFAULT_MAX_RETRY;
        int nextRetry = (current.getRetryCount() == null ? 0 : current.getRetryCount()) + 1;
        String status = nextRetry >= resolvedMaxRetry
                ? CrmOaSyncStatusEnum.FAILED.getCode()
                : CrmOaSyncStatusEnum.RETRY.getCode();
        String truncatedError = truncate(errorMsg, 2000);
        crmOrderDetailMapper.update(null, new LambdaUpdateWrapper<CrmOrderDetailDO>()
                .eq(CrmOrderDetailDO::getId, detailId)
                .set(CrmOrderDetailDO::getOaSyncStatus, status)
                .set(CrmOrderDetailDO::getRetryCount, nextRetry)
                .set(CrmOrderDetailDO::getLastErrorMsg, truncatedError)
                .set(CrmOrderDetailDO::getOaSyncTime, LocalDateTime.now()));
        log.info("【CrmOrderDetail】OA同步失败回写, detailId={}, status={}, retryCount={}, error={}",
                detailId, status, nextRetry, truncatedError);
        return status;
    }

    private static String truncate(String text, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }
}
