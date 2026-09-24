package com.cacch.integration.service.ihr.api.impl;

import com.cacch.integration.mapper.ihr.PersondetailMapper;
import com.cacch.integration.service.ihr.api.IIhrPersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * IHR 人员查询服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IhrPersonServiceImpl implements IIhrPersonService {

    private static final String BIZ = "IHR 人员查询 Service";

    private final PersondetailMapper persondetailMapper;

    @Override
    public List<Map<String, Object>> listStaffByDeptTree(String ihrDeptId, LocalDate lastUpdateDate) {
        if (ihrDeptId == null || ihrDeptId.isBlank()) {
            log.info("【{}】listStaffByDeptTree 跳过, ihrDeptId 为空", BIZ);
            return Collections.emptyList();
        }
        log.info("【{}】listStaffByDeptTree 查询, ihrDeptId={}, lastUpdateDate={}", BIZ, ihrDeptId, lastUpdateDate);
        List<Map<String, Object>> list = persondetailMapper.selectByDeptTree(ihrDeptId, lastUpdateDate);
        if (list == null || list.isEmpty()) {
            log.info("【{}】listStaffByDeptTree 返回空, ihrDeptId={}, lastUpdateDate={}", BIZ, ihrDeptId, lastUpdateDate);
            return Collections.emptyList();
        }
        log.info("【{}】listStaffByDeptTree 命中, ihrDeptId={}, count={}", BIZ, ihrDeptId, list.size());
        return list;
    }
}
