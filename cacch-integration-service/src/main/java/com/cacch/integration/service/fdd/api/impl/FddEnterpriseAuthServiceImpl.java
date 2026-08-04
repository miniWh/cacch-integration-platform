package com.cacch.integration.service.fdd.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.common.enums.fdd.FddAuthStatusEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.fdd.FddEnterpriseAuthDO;
import com.cacch.integration.mapper.fdd.FddEnterpriseAuthMapper;
import com.cacch.integration.service.fdd.api.IFddEnterpriseAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 法大大企业实名认证服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FddEnterpriseAuthServiceImpl implements IFddEnterpriseAuthService {

    private static final String LOG_BIZ = "FddEnterpriseAuth";

    private final FddEnterpriseAuthMapper fddEnterpriseAuthMapper;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public FddEnterpriseAuthDO findSuccess(String internalCompanyName, String uscc) {
        if (!StringUtils.hasText(internalCompanyName) || !StringUtils.hasText(uscc)) {
            return null;
        }
        return fddEnterpriseAuthMapper.selectOne(new LambdaQueryWrapper<FddEnterpriseAuthDO>()
                .eq(FddEnterpriseAuthDO::getInternalCompanyName, internalCompanyName.trim())
                .eq(FddEnterpriseAuthDO::getUscc, uscc.trim())
                .eq(FddEnterpriseAuthDO::getAuthStatus, FddAuthStatusEnum.SUCCESS.getCode())
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public FddEnterpriseAuthDO findLatestPending(String internalCompanyName, String uscc) {
        if (!StringUtils.hasText(internalCompanyName) || !StringUtils.hasText(uscc)) {
            return null;
        }
        return fddEnterpriseAuthMapper.selectOne(new LambdaQueryWrapper<FddEnterpriseAuthDO>()
                .eq(FddEnterpriseAuthDO::getInternalCompanyName, internalCompanyName.trim())
                .eq(FddEnterpriseAuthDO::getUscc, uscc.trim())
                .eq(FddEnterpriseAuthDO::getAuthStatus, FddAuthStatusEnum.PENDING.getCode())
                .orderByDesc(FddEnterpriseAuthDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public FddEnterpriseAuthDO findLatestFailed(String internalCompanyName, String uscc) {
        if (!StringUtils.hasText(internalCompanyName) || !StringUtils.hasText(uscc)) {
            return null;
        }
        return fddEnterpriseAuthMapper.selectOne(new LambdaQueryWrapper<FddEnterpriseAuthDO>()
                .eq(FddEnterpriseAuthDO::getInternalCompanyName, internalCompanyName.trim())
                .eq(FddEnterpriseAuthDO::getUscc, uscc.trim())
                .eq(FddEnterpriseAuthDO::getAuthStatus, FddAuthStatusEnum.FAILED.getCode())
                .orderByDesc(FddEnterpriseAuthDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public boolean hasFailedHistory(String internalCompanyName, String uscc) {
        if (!StringUtils.hasText(internalCompanyName) || !StringUtils.hasText(uscc)) {
            return false;
        }
        Long count = fddEnterpriseAuthMapper.selectCount(new LambdaQueryWrapper<FddEnterpriseAuthDO>()
                .eq(FddEnterpriseAuthDO::getInternalCompanyName, internalCompanyName.trim())
                .eq(FddEnterpriseAuthDO::getUscc, uscc.trim())
                .eq(FddEnterpriseAuthDO::getAuthStatus, FddAuthStatusEnum.FAILED.getCode()));
        return count != null && count > 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public FddEnterpriseAuthDO insertPending(FddEnterpriseAuthDO record) {
        if (record == null
                || !StringUtils.hasText(record.getInternalCompanyName())
                || !StringUtils.hasText(record.getUscc())
                || !StringUtils.hasText(record.getEnterpriseName())
                || !StringUtils.hasText(record.getSourceSystem())) {
            log.info("【{}】新增 PENDING 终止, reason=必填字段缺失", LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "企业认证记录必填字段缺失");
        }
        record.setAuthStatus(FddAuthStatusEnum.PENDING.getCode());
        fddEnterpriseAuthMapper.insert(record);
        log.info("【{}】新增 PENDING 成功, id={}, internalCompany={}, uscc={}, transactionNo={}",
                LOG_BIZ, record.getId(), record.getInternalCompanyName(), record.getUscc(), record.getTransactionNo());
        return record;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public FddEnterpriseAuthDO findByTransactionNo(String transactionNo) {
        if (!StringUtils.hasText(transactionNo)) {
            return null;
        }
        return fddEnterpriseAuthMapper.selectOne(new LambdaQueryWrapper<FddEnterpriseAuthDO>()
                .eq(FddEnterpriseAuthDO::getTransactionNo, transactionNo.trim())
                .orderByDesc(FddEnterpriseAuthDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public void updateByCallback(Long id, String authStatus, Object authDetail, String failReason) {
        if (id == null || !StringUtils.hasText(authStatus)) {
            log.info("【{}】回调更新终止, reason=参数无效, id={}", LOG_BIZ, id);
            throw new BizException(ResultCode.PARAM_INVALID, "回调更新参数无效");
        }
        FddEnterpriseAuthDO existing = fddEnterpriseAuthMapper.selectById(id);
        if (existing == null) {
            log.info("【{}】回调更新终止, reason=记录不存在, id={}", LOG_BIZ, id);
            throw new BizException(ResultCode.PARAM_INVALID, "认证记录不存在, id=" + id);
        }
        existing.setAuthStatus(authStatus);
        existing.setAuthDetail(authDetail);
        existing.setFailReason(failReason);
        if (FddAuthStatusEnum.SUCCESS.getCode().equals(authStatus)) {
            existing.setCertifiedAt(LocalDateTime.now());
        }
        fddEnterpriseAuthMapper.updateById(existing);
        log.info("【{}】回调更新完成, id={}, authStatus={}", LOG_BIZ, id, authStatus);
    }
}
