package com.cacch.integration.manager.ihr.api.impl;

import com.cacch.integration.manager.ihr.api.IIhrPersonManager;
import com.cacch.integration.service.ihr.api.IIhrPersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * IHR 人员查询编排实现
 *
 * <p>仅做查询委派，无跨聚合编排、无事务控制、无 HTTP 调用。
 * 当前实现直接委托 {@link IIhrPersonService}，保留 Manager 层为后续扩展（如多源聚合、缓存）预留位置。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrPersonManagerImpl implements IIhrPersonManager {

    private static final String BIZ = "IHR 人员查询编排";

    private final IIhrPersonService ihrPersonService;

    @Override
    public List<Map<String, Object>> listStaffByDeptTree(String ihrDeptId, LocalDate lastUpdateDate) {
        log.info("【{}】listStaffByDeptTree 入口, ihrDeptId={}, lastUpdateDate={}", BIZ, ihrDeptId, lastUpdateDate);
        return ihrPersonService.listStaffByDeptTree(ihrDeptId, lastUpdateDate);
    }
}
