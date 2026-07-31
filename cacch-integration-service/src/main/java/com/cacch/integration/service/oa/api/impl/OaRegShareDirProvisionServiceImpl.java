package com.cacch.integration.service.oa.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cacch.integration.common.constant.oa.OaRegReportConstants;
import com.cacch.integration.common.constant.oa.ShareDirProvisionConstants;
import com.cacch.integration.entity.oa.OaRegShareDirProvisionDO;
import com.cacch.integration.mapper.oa.OaRegShareDirProvisionMapper;
import com.cacch.integration.service.oa.api.IOaRegShareDirProvisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 共享盘目录治理记录服务实现（REQ-OA-002）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaRegShareDirProvisionServiceImpl implements IOaRegShareDirProvisionService {

    private static final String BIZ = ShareDirProvisionConstants.LOG_BIZ;

    private final OaRegShareDirProvisionMapper provisionMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 60, rollbackFor = Exception.class)
    public int batchInsert(List<OaRegShareDirProvisionDO> records) {
        if (records == null || records.isEmpty()) {
            log.info("【{}】批量写入终止, reason=记录列表为空", BIZ);
            return 0;
        }
        int count = 0;
        for (OaRegShareDirProvisionDO record : records) {
            if (record == null) {
                continue;
            }
            provisionMapper.insert(record);
            count++;
        }
        log.info("【{}】批量写入治理记录完成, runId={}, count={}", BIZ,
                records.get(0).getRunId(), count);
        return count;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 15, rollbackFor = Exception.class)
    public List<OaRegShareDirProvisionDO> findByRunId(String runId) {
        if (!StringUtils.hasText(runId)) {
            log.info("【{}】按runId查询终止, reason=runId为空", BIZ);
            return Collections.emptyList();
        }
        return provisionMapper.selectList(new LambdaQueryWrapper<OaRegShareDirProvisionDO>()
                .eq(OaRegShareDirProvisionDO::getRunId, runId.trim())
                .orderByDesc(OaRegShareDirProvisionDO::getProvisionedAt)
                .orderByDesc(OaRegShareDirProvisionDO::getId));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 15, rollbackFor = Exception.class)
    public long countByRunIdAndAction(String runId, String action) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(action)) {
            return 0;
        }
        return provisionMapper.selectCount(new LambdaQueryWrapper<OaRegShareDirProvisionDO>()
                .eq(OaRegShareDirProvisionDO::getRunId, runId.trim())
                .eq(OaRegShareDirProvisionDO::getAction, action.trim()));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 15, rollbackFor = Exception.class)
    public IPage<OaRegShareDirProvisionDO> pageQuery(String runId,
                                                     String ownerName,
                                                     String action,
                                                     long page,
                                                     long size) {
        long resolvedPage = page > 0 ? page : 1;
        long resolvedSize = resolvePageSize(size);
        LambdaQueryWrapper<OaRegShareDirProvisionDO> wrapper = new LambdaQueryWrapper<OaRegShareDirProvisionDO>()
                .orderByDesc(OaRegShareDirProvisionDO::getProvisionedAt)
                .orderByDesc(OaRegShareDirProvisionDO::getId);
        if (StringUtils.hasText(runId)) {
            wrapper.eq(OaRegShareDirProvisionDO::getRunId, runId.trim());
        }
        if (StringUtils.hasText(ownerName)) {
            wrapper.like(OaRegShareDirProvisionDO::getOwnerName, ownerName.trim());
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(OaRegShareDirProvisionDO::getAction, action.trim());
        }
        return provisionMapper.selectPage(new Page<>(resolvedPage, resolvedSize), wrapper);
    }

    private static long resolvePageSize(long size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, OaRegReportConstants.MAX_PAGE_SIZE);
    }
}
