package com.cacch.integration.manager.moka.api.impl;

import org.apache.commons.lang3.StringUtils;

import com.cacch.integration.entity.ihr.IhrDepartmentDO;
import com.cacch.integration.entity.moka.MokaDepartmentDO;
import com.cacch.integration.entity.moka.MokaDepartmentLocalizedDO;
import com.cacch.integration.manager.moka.api.IMokaDepartmentSyncManager;
import com.cacch.integration.service.ihr.api.IIhrDepartmentService;
import com.cacch.integration.service.moka.api.IMokaDepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Moka 部门同步编排实现 — 从本地 IHR 部门快照表读取启用状态部门，映射后批量 upsert 到 Moka 表
 *
 * <p>调用链：Controller → syncFromIhr() → IIhrDepartmentService.listEnabled()
 * （一次性读取 {@code t_integration_ihr_department} 中 {@code department_status='ENABLE'} 的记录）
 * → 字段映射 → IMokaDepartmentService.batchUpsert()（主表）+ batchUpsertLocalized()（子表）</p>
 *
 * <p>事务策略：每次批量 upsert 独立事务（由 Service 内部 {@code @Transactional} 控制），
 * Manager 层不在外层包裹大事务，避免长事务锁表。</p>
 *
 * <p>变更说明：原实现通过 {@code IIhrOrgManager.searchDepartments()} 循环翻页回源调用 IHR 开放平台，
 * 现改为只读本地快照表。IHR 数据刷新由独立的 {@code POST /api/v1/ihr/departments/sync} 接口负责。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaDepartmentSyncManagerImpl implements IMokaDepartmentSyncManager {

    private static final String BIZ = "Moka部门同步编排";

    /**
     * moka_sync_status 常量：3-待删除，4-已删除
     */
    private static final int STATUS_PENDING_DELETE = 3;
    private static final int STATUS_DELETED = 4;

    /**
     * 子表 locale 固定值
     */
    private static final String LOCALE_ZH_CN = "zh_CN";

    /**
     * iHR 根部门 parent_code 兜底值
     */
    private static final String ROOT_PARENT_CODE = "0";

    private final IIhrDepartmentService ihrDepartmentService;
    private final IMokaDepartmentService mokaDepartmentService;

    @Override
    public MokaDeptSyncResult syncFromIhr() {
        log.info("【{}】开始从本地 IHR 部门快照表同步启用状态部门到 Moka 表", BIZ);

        // 一次性读取本地 IHR 部门快照表（department_status='ENABLE' 且未逻辑删除）
        List<IhrDepartmentDO> ihrDepts = ihrDepartmentService.listEnabled();
        int totalFetched = ihrDepts.size();
        log.info("【{}】本地 IHR 部门快照命中, count={}", BIZ, totalFetched);

        if (ihrDepts.isEmpty()) {
            log.info("【{}】本地 IHR 快照为空, 同步结束（请先调用 POST /api/v1/ihr/departments/sync 刷新快照）", BIZ);
            return new MokaDeptSyncResult(0, 0, 0, 0, 0);
        }

        // 逐条转换并加入批次
        List<MokaDepartmentDO> deptBatch = new ArrayList<>(totalFetched);
        List<MokaDepartmentLocalizedDO> localBatch = new ArrayList<>(totalFetched);
        int deptSkipped = 0;

        for (IhrDepartmentDO ihrDept : ihrDepts) {
            // 校验：departmentCode 为空则跳过
            if (StringUtils.isBlank(ihrDept.getDepartmentCode())) {
                deptSkipped++;
                log.info("【{}】跳过无 departmentCode 的部门, uuid={}, name={}",
                        BIZ, ihrDept.getUuid(), ihrDept.getName());
                continue;
            }

            // 主表映射
            MokaDepartmentDO mokaDept = mapMainTable(ihrDept);
            deptBatch.add(mokaDept);

            // 子表映射（固定 zh_CN，prop_value = name）
            MokaDepartmentLocalizedDO localized = new MokaDepartmentLocalizedDO();
            localized.setDepartmentCode(mokaDept.getDepartmentCode());
            localized.setLocale(LOCALE_ZH_CN);
            localized.setPropValue(mokaDept.getName());
            localBatch.add(localized);
        }

        if (deptBatch.isEmpty()) {
            log.info("【{}】本地 IHR 快照无有效部门（全部缺 departmentCode）, 同步结束, totalFetched={}, skipped={}",
                    BIZ, totalFetched, deptSkipped);
            return new MokaDeptSyncResult(totalFetched, 0, 0, deptSkipped, 0);
        }

        // 批量落库
        log.info("【{}】开始批量落库, totalFetched={}, validToUpsert={}, skipped={}",
                BIZ, totalFetched, deptBatch.size(), deptSkipped);

        int deptUpserted = mokaDepartmentService.batchUpsert(deptBatch);
        int localizedUpserted = mokaDepartmentService.batchUpsertLocalized(localBatch);

        // 差集标记待删除：本地 Moka 表中不在 IHR ENABLE 集合、且状态非 3/4 的部门 → 置 3（待删除）
        Set<String> ihrEnabledCodes = new HashSet<>();
        for (IhrDepartmentDO ihrDept : ihrDepts) {
            if (StringUtils.isNotBlank(ihrDept.getDepartmentCode())) {
                ihrEnabledCodes.add(ihrDept.getDepartmentCode());
            }
        }
        int markedDeleteCount = markPendingDeletes(ihrEnabledCodes);

        log.info("【{}】同步完成, totalFetched={}, deptUpserted={}, localizedUpserted={}, deptSkipped={}, markedDelete={}",
                BIZ, totalFetched, deptUpserted, localizedUpserted, deptSkipped, markedDeleteCount);

        return new MokaDeptSyncResult(totalFetched, deptUpserted, localizedUpserted, deptSkipped, markedDeleteCount);
    }

    /**
     * 差集计算并标记待删除部门
     *
     * <p>本地 Moka 表中 departmentCode 不在 IHR ENABLE 集合、且 moka_sync_status 非 3（待删除）
     * / 4（已删除）的部门，逐条置 3（待删除）。单条失败不阻断批次（try-catch 累计）。</p>
     *
     * <p>iHR 重新启用的部门会在 upsert 阶段复活（状态重置 0、moka_delete_time 置 NULL），
     * 不会进入待删除集合。</p>
     *
     * @param ihrEnabledCodes IHR ENABLE 状态部门的 departmentCode 集合
     * @return 成功标记为待删除的条数
     */
    private int markPendingDeletes(Set<String> ihrEnabledCodes) {
        List<MokaDepartmentDO> localDepts = mokaDepartmentService.listAll();
        int marked = 0;
        int failed = 0;

        for (MokaDepartmentDO dept : localDepts) {
            if (ihrEnabledCodes.contains(dept.getDepartmentCode())) {
                continue;
            }
            Integer status = dept.getMokaSyncStatus();
            if (status != null && (status == STATUS_PENDING_DELETE || status == STATUS_DELETED)) {
                continue;
            }
            try {
                mokaDepartmentService.updateMokaSyncStatus(dept.getDepartmentCode(), STATUS_PENDING_DELETE);
                marked++;
                log.info("【{}】标记待删除, departmentCode={}, name={}",
                        BIZ, dept.getDepartmentCode(), dept.getName());
            } catch (Exception e) {
                failed++;
                log.error("【{}】标记待删除失败, departmentCode={}, 已失败={}",
                        BIZ, dept.getDepartmentCode(), failed, e);
            }
        }

        if (marked > 0 || failed > 0) {
            log.info("【{}】差集标记待删除完成, marked={}, failed={}", BIZ, marked, failed);
        } else {
            log.info("【{}】无差集部门, 未产生待删除标记", BIZ);
        }
        return marked;
    }

    // —— 字段映射 ——

    /**
     * IhrDepartmentDO → MokaDepartmentDO 字段映射
     *
     * <p>映射规则：
     * <ul>
     *     <li>departmentCode → department_code（主键）</li>
     *     <li>name → name</li>
     *     <li>parentDepartmentCode → parent_code（null/空兜底 "0"）</li>
     *     <li>type（COMPANY/DEPARTMENT/STORE）→ type（1/1/2）</li>
     *     <li>sequence → sequence（Integer 直接赋值，默认 null → 9999）</li>
     *     <li>mokaSyncStatus → 0（PENDING，未同步到 Moka 开放平台）</li>
     * </ul>
     */
    private MokaDepartmentDO mapMainTable(IhrDepartmentDO ihr) {
        MokaDepartmentDO moka = new MokaDepartmentDO();

        moka.setDepartmentCode(ihr.getDepartmentCode());
        moka.setName(ihr.getName());

        // parent_code：parentDepartmentCode 为 null/空 时兜底 "0"（根部门）
        String parentDeptCode = ihr.getParentDepartmentCode();
        moka.setParentCode((StringUtils.isBlank(parentDeptCode))
                ? ROOT_PARENT_CODE
                : parentDeptCode);

        // type 枚举转换
        moka.setType(convertType(ihr.getType()));

        // sequence：iHR 为 Integer，直接赋值；null 时兜底 9999（排到最后）
        moka.setSequence(ihr.getSequence() != null ? ihr.getSequence() : 9999);

        // Moka 开放平台同步状态：默认 0-未同步，等后续调 Moka API 写入成功后置 1
        moka.setMokaSyncStatus(0);

        return moka;
    }

    /**
     * iHR type 枚举 → Moka type 数值
     *
     * <p>iHR 取值：COMPANY / DEPARTMENT / STORE / 其他；Moka 取值：1-普通部门 / 2-门店部门</p>
     */
    private Integer convertType(String ihrType) {
        if (ihrType == null) {
            return 1;
        }
        return switch (ihrType.toUpperCase()) {
            case "STORE" -> 2;
            default -> 1;  // COMPANY / DEPARTMENT / 其他 → 1（普通部门）
        };
    }
}
