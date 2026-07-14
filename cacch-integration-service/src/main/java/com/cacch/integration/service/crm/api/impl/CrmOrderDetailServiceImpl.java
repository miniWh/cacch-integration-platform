package com.cacch.integration.service.crm.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
}
