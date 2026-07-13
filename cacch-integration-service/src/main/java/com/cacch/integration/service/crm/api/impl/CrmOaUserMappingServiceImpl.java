package com.cacch.integration.service.crm.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.crm.CrmOaUserMappingDO;
import com.cacch.integration.mapper.crm.CrmOaUserMappingMapper;
import com.cacch.integration.service.crm.api.ICrmOaUserMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * CRM↔OA 人员映射持久化服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmOaUserMappingServiceImpl implements ICrmOaUserMappingService {

    private final CrmOaUserMappingMapper crmOaUserMappingMapper;

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public CrmOaUserMappingDO getByCrmEmployeeId(String crmEmployeeId) {
        if (!StringUtils.hasText(crmEmployeeId)) {
            return null;
        }
        return crmOaUserMappingMapper.selectOne(new LambdaQueryWrapper<CrmOaUserMappingDO>()
                .eq(CrmOaUserMappingDO::getCrmEmployeeId, crmEmployeeId.trim())
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public CrmOaUserMappingDO saveOrUpdateByCrmEmployeeId(CrmOaUserMappingDO mapping) {
        if (mapping == null || !StringUtils.hasText(mapping.getCrmEmployeeId())) {
            log.info("【CrmOaUserMapping】UPSERT终止, reason=crmEmployeeId为空");
            throw new BizException(ResultCode.PARAM_MISSING, "crmEmployeeId 不能为空");
        }
        String crmEmployeeId = mapping.getCrmEmployeeId().trim();
        mapping.setCrmEmployeeId(crmEmployeeId);
        mapping.setLastMappedAt(LocalDateTime.now());

        CrmOaUserMappingDO existing = getByCrmEmployeeId(crmEmployeeId);
        if (existing == null) {
            crmOaUserMappingMapper.insert(mapping);
            log.info("【CrmOaUserMapping】新建映射, crmEmployeeId={}, empCode={}, oaUserId={}",
                    crmEmployeeId, mapping.getEmpCode(), mapping.getOaUserId());
            return mapping;
        }

        existing.setEmpCode(mapping.getEmpCode());
        existing.setOaUserId(mapping.getOaUserId());
        existing.setOaLoginName(mapping.getOaLoginName());
        existing.setCrmEmployeeName(mapping.getCrmEmployeeName());
        existing.setCrmRawPayload(mapping.getCrmRawPayload());
        existing.setOaRawPayload(mapping.getOaRawPayload());
        existing.setLastMappedAt(mapping.getLastMappedAt());
        crmOaUserMappingMapper.updateById(existing);
        log.info("【CrmOaUserMapping】更新映射, crmEmployeeId={}, empCode={}, oaUserId={}, id={}",
                crmEmployeeId, existing.getEmpCode(), existing.getOaUserId(), existing.getId());
        return existing;
    }
}
