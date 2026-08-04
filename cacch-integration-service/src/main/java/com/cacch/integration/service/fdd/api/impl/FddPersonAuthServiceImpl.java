package com.cacch.integration.service.fdd.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.common.enums.fdd.FddAuthStatusEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.fdd.FddPersonAuthDO;
import com.cacch.integration.mapper.fdd.FddPersonAuthMapper;
import com.cacch.integration.service.fdd.api.IFddPersonAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 法大大个人实名认证服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FddPersonAuthServiceImpl implements IFddPersonAuthService {

    private static final String LOG_BIZ = "FddPersonAuth";

    private final FddPersonAuthMapper fddPersonAuthMapper;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public FddPersonAuthDO findSuccess(String internalCompanyName, String idNumber) {
        if (!StringUtils.hasText(internalCompanyName) || !StringUtils.hasText(idNumber)) {
            return null;
        }
        return fddPersonAuthMapper.selectOne(new LambdaQueryWrapper<FddPersonAuthDO>()
                .eq(FddPersonAuthDO::getInternalCompanyName, internalCompanyName.trim())
                .eq(FddPersonAuthDO::getIdNumber, idNumber.trim())
                .eq(FddPersonAuthDO::getAuthStatus, FddAuthStatusEnum.SUCCESS.getCode())
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public FddPersonAuthDO findLatestPending(String internalCompanyName, String idNumber) {
        if (!StringUtils.hasText(internalCompanyName) || !StringUtils.hasText(idNumber)) {
            return null;
        }
        return fddPersonAuthMapper.selectOne(new LambdaQueryWrapper<FddPersonAuthDO>()
                .eq(FddPersonAuthDO::getInternalCompanyName, internalCompanyName.trim())
                .eq(FddPersonAuthDO::getIdNumber, idNumber.trim())
                .eq(FddPersonAuthDO::getAuthStatus, FddAuthStatusEnum.PENDING.getCode())
                .orderByDesc(FddPersonAuthDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public FddPersonAuthDO findLatestFailed(String internalCompanyName, String idNumber) {
        if (!StringUtils.hasText(internalCompanyName) || !StringUtils.hasText(idNumber)) {
            return null;
        }
        return fddPersonAuthMapper.selectOne(new LambdaQueryWrapper<FddPersonAuthDO>()
                .eq(FddPersonAuthDO::getInternalCompanyName, internalCompanyName.trim())
                .eq(FddPersonAuthDO::getIdNumber, idNumber.trim())
                .eq(FddPersonAuthDO::getAuthStatus, FddAuthStatusEnum.FAILED.getCode())
                .orderByDesc(FddPersonAuthDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public boolean hasFailedHistory(String internalCompanyName, String idNumber) {
        if (!StringUtils.hasText(internalCompanyName) || !StringUtils.hasText(idNumber)) {
            return false;
        }
        Long count = fddPersonAuthMapper.selectCount(new LambdaQueryWrapper<FddPersonAuthDO>()
                .eq(FddPersonAuthDO::getInternalCompanyName, internalCompanyName.trim())
                .eq(FddPersonAuthDO::getIdNumber, idNumber.trim())
                .eq(FddPersonAuthDO::getAuthStatus, FddAuthStatusEnum.FAILED.getCode()));
        return count != null && count > 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public FddPersonAuthDO insertPending(FddPersonAuthDO record) {
        if (record == null
                || !StringUtils.hasText(record.getInternalCompanyName())
                || !StringUtils.hasText(record.getIdNumber())
                || !StringUtils.hasText(record.getPersonName())
                || !StringUtils.hasText(record.getSourceSystem())) {
            log.info("【{}】新增 PENDING 终止, reason=必填字段缺失", LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "个人认证记录必填字段缺失");
        }
        record.setAuthStatus(FddAuthStatusEnum.PENDING.getCode());
        fddPersonAuthMapper.insert(record);
        log.info("【{}】新增 PENDING 成功, id={}, internalCompany={}, idNumber={}, transactionNo={}",
                LOG_BIZ, record.getId(), record.getInternalCompanyName(),
                maskIdNumber(record.getIdNumber()), record.getTransactionNo());
        return record;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public FddPersonAuthDO findByTransactionNo(String transactionNo) {
        if (!StringUtils.hasText(transactionNo)) {
            return null;
        }
        return fddPersonAuthMapper.selectOne(new LambdaQueryWrapper<FddPersonAuthDO>()
                .eq(FddPersonAuthDO::getTransactionNo, transactionNo.trim())
                .orderByDesc(FddPersonAuthDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public void updateByCallback(Long id, String authStatus, Object authDetail, String failReason) {
        if (id == null || !StringUtils.hasText(authStatus)) {
            log.info("【{}】回调更新终止, reason=参数无效, id={}", LOG_BIZ, id);
            throw new BizException(ResultCode.PARAM_INVALID, "回调更新参数无效");
        }
        FddPersonAuthDO existing = fddPersonAuthMapper.selectById(id);
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
        fddPersonAuthMapper.updateById(existing);
        log.info("【{}】回调更新完成, id={}, authStatus={}", LOG_BIZ, id, authStatus);
    }

    private static String maskIdNumber(String idNumber) {
        if (!StringUtils.hasText(idNumber) || idNumber.length() < 10) {
            return "****";
        }
        return idNumber.substring(0, 6) + "********" + idNumber.substring(idNumber.length() - 4);
    }
}
