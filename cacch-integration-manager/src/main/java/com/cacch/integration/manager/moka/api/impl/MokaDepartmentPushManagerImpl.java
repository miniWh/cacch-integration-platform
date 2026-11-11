package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.moka.MokaDepartmentDO;
import com.cacch.integration.entity.moka.MokaDepartmentLocalizedDO;
import com.cacch.integration.integration.moka.client.dto.MokaDeptDeleteRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDepartment;
import com.cacch.integration.manager.moka.api.IMokaDepartmentPushManager;
import com.cacch.integration.service.moka.api.IMokaDepartmentService;
import com.cacch.integration.service.moka.api.IMokaOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Moka 部门推送实现 —— 本地 PG 表 → Moka 开放平台 API
 *
 * <p>执行流程分两步：
 * <ol>
 *     <li>前置删除步骤（processPendingDeletes）：处理 {@code moka_sync_status=3}（待删除）的部门 ——
 *     {@code moka.delete-email} 未配置时跳过；孤儿校验（存在未删除子部门的跳过该部门）；
 *     可删部门按树深度自底向上排序后调 Moka DELETE /api-platform/v1/departments 标记删除；
 *     成功逐条置 4（已删除，记录 moka_delete_time），失败保留 3 待下轮重试。</li>
 *     <li>PUT 全量同步：Moka PUT /api-platform/v2/departments 以 departmentCode 为主键，
 *     本次传入的 departments 即 Moka 侧完整最新列表。因此推送范围必须是本地 PG 全量
 *     （排除待删除 3 / 已删除 4），不能只推 PENDING/FAILED 的子集
 *     （否则 Moka 会把未传入的已有部门标记删除）。</li>
 * </ol>
 *
 * <p>moka_sync_status 更新策略：
 * <ul>
 *     <li>PUT 返回成功 → 推送部门全部 markMokaSynced（状态 1 + moka_sync_time）</li>
 *     <li>PUT 调用异常 / code≠0 → 推送部门全部置 2（SYNC_FAILED）</li>
 *     <li>DELETE 成功 → 待删部门全部 markMokaDeleted（状态 4 + moka_delete_time）</li>
 * </ul>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaDepartmentPushManagerImpl implements IMokaDepartmentPushManager {

    private static final String BIZ = "Moka部门推送编排";

    /**
     * moka_sync_status 常量
     */
    private static final int STATUS_SYNCED = 1;
    private static final int STATUS_SYNC_FAILED = 2;
    private static final int STATUS_PENDING_DELETE = 3;
    private static final int STATUS_DELETED = 4;

    /**
     * 根部门 parent_code 哨兵值（深度计算的递归终止条件）
     */
    private static final String ROOT_PARENT_CODE = "0";

    private final IMokaDepartmentService mokaDepartmentService;
    private final IMokaOrgService mokaOrgService;
    private final MokaProperties mokaProperties;

    @Override
    public MokaDeptPushResult pushToMoka() {
        // 1) 前置：处理待删除部门（状态 3）
        DeleteOutcome deleteOutcome = processPendingDeletes();

        // 2) 查询本地 PG 全量部门，排除待删除（3）/已删除（4）
        List<MokaDepartmentDO> allDepts = mokaDepartmentService.listAll();
        List<MokaDepartmentDO> localDepts = allDepts.stream()
                .filter(dept -> {
                    Integer status = dept.getMokaSyncStatus();
                    return status == null || (status != STATUS_PENDING_DELETE && status != STATUS_DELETED);
                })
                .toList();
        log.info("【{}】查询本地 PG 部门全量, totalCount={}, pushableCount={}",
                BIZ, allDepts.size(), localDepts.size());

        if (localDepts.isEmpty()) {
            log.info("【{}】本地 PG 无可推送部门数据, 跳过推送", BIZ);
            return new MokaDeptPushResult(0, 0, false, null, null, null,
                    0, 0, 0,
                    deleteOutcome.pendingCount(), deleteOutcome.skippedCount(),
                    deleteOutcome.success(), deleteOutcome.deletedCount());
        }

        // 3) 转换为 Moka API 请求体（含多语言子表数据）
        List<MokaDepartment> apiDepts = convertToApiDepartments(localDepts);
        MokaDeptSyncRequest request = new MokaDeptSyncRequest();
        request.setDepartments(apiDepts);

        // 4) 调用 Moka 开放平台全量同步 API
        MokaDeptSyncResponse response;
        boolean mokaSuccess = false;
        Integer newCount = null;
        Integer updateCount = null;
        Integer deleteCount = null;

        try {
            log.info("【{}】开始推送 Moka, deptCount={}", BIZ, apiDepts.size());
            response = mokaOrgService.syncDepartmentsFull(request);
            mokaSuccess = true;
            newCount = response.getNewCount();
            updateCount = response.getUpdateCount();
            deleteCount = response.getDeleteCount();
            log.info("【{}】Moka 推送成功, new={}, update={}, delete={}",
                    BIZ, newCount, updateCount, deleteCount);

        } catch (BizException | RestClientException e) {
            log.info("【{}】Moka 推送终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】Moka API 调用失败", BIZ, e);
        }

        // 5) 更新本地 moka_sync_status（成功走 markMokaSynced 记录 moka_sync_time）
        int[] statusResult = bulkUpdatePushStatus(localDepts, mokaSuccess);
        // statusResult[0] = 更新影响行数, statusResult[1] = DB 异常条数
        int updated = statusResult[0];
        int dbUpdateFailed = statusResult[1];
        int syncedCount = mokaSuccess ? updated : 0;
        int syncFailedCount = mokaSuccess ? 0 : updated;
        log.info("【{}】更新本地同步状态为 {}, 更新条数={}, DB异常={}",
                BIZ, mokaSuccess ? STATUS_SYNCED : STATUS_SYNC_FAILED, updated, dbUpdateFailed);

        return new MokaDeptPushResult(
                localDepts.size(),
                localDepts.size(),
                mokaSuccess,
                newCount, updateCount, deleteCount,
                syncedCount, syncFailedCount, dbUpdateFailed,
                deleteOutcome.pendingCount(), deleteOutcome.skippedCount(),
                deleteOutcome.success(), deleteOutcome.deletedCount()
        );
    }

    // —— 手动删除编排 ——

    /**
     * 手动触发部门删除 —— 编码校验 + 标记待删除 + 立即执行删除编排
     *
     * <p>编码校验（逐条，单条失败不阻断）：
     * 去重 → 本地不存在计 notFound → 状态 4（已删除）跳过标记计 alreadyDeleted →
     * 状态 0/1/2 置 3（待删除）→ 状态已是 3 的无需重复标记。</p>
     *
     * <p>标记完成后立即调用 {@link #processPendingDeletes()}（与 pushToMoka 前置删除步骤
     * 同一套编排，处理全部状态 3 部门，含此前差集标记的历史待删部门）。</p>
     *
     * @param departmentCodes 待删除部门编码列表，不可为空
     * @return 手动删除执行结果
     */
    @Override
    public MokaManualDeleteResult deleteDepartments(List<String> departmentCodes) {
        // 1) 编码去重（LinkedHashSet 保持传入顺序）
        List<String> distinctCodes = departmentCodes.stream().distinct().toList();
        int requestCount = distinctCodes.size();
        log.info("【{}】手动删除部门开始, requestCount={}", BIZ, requestCount);

        // 2) 逐条校验并标记
        int notFoundCount = 0;
        int alreadyDeletedCount = 0;
        int markedCount = 0;
        int markFailedCount = 0;

        for (String departmentCode : distinctCodes) {
            MokaDepartmentDO dept = mokaDepartmentService.getByCode(departmentCode);
            if (dept == null) {
                notFoundCount++;
                log.info("【{}】部门编码不存在, 跳过, departmentCode={}", BIZ, departmentCode);
                continue;
            }

            Integer status = dept.getMokaSyncStatus();
            if (status != null && status == STATUS_DELETED) {
                alreadyDeletedCount++;
                log.info("【{}】部门已是已删除状态, 跳过标记, departmentCode={}, name={}",
                        BIZ, departmentCode, dept.getName());
                continue;
            }
            if (status != null && status == STATUS_PENDING_DELETE) {
                log.info("【{}】部门已是待删除状态, 无需重复标记, departmentCode={}, name={}",
                        BIZ, departmentCode, dept.getName());
                continue;
            }

            try {
                mokaDepartmentService.updateMokaSyncStatus(departmentCode, STATUS_PENDING_DELETE);
                markedCount++;
                log.info("【{}】手动标记待删除, departmentCode={}, name={}",
                        BIZ, departmentCode, dept.getName());
            } catch (Exception e) {
                markFailedCount++;
                log.error("【{}】手动标记待删除失败, departmentCode={}, 已失败={}",
                        BIZ, departmentCode, markFailedCount, e);
            }
        }
        log.info("【{}】手动标记完成, requestCount={}, notFound={}, alreadyDeleted={}, marked={}, markFailed={}",
                BIZ, requestCount, notFoundCount, alreadyDeletedCount, markedCount, markFailedCount);

        // 3) 立即执行删除编排（处理全部状态 3 部门，含历史待删）
        DeleteOutcome outcome = processPendingDeletes();

        log.info("【{}】手动删除部门完成, deletePendingCount={}, deleteSkippedCount={}, deleteSuccess={}, deletedCount={}",
                BIZ, outcome.pendingCount(), outcome.skippedCount(), outcome.success(), outcome.deletedCount());

        return new MokaManualDeleteResult(
                requestCount, notFoundCount, alreadyDeletedCount, markedCount,
                outcome.pendingCount(), outcome.skippedCount(),
                outcome.success(), outcome.deletedCount());
    }

    // —— 前置删除步骤 ——

    /**
     * 处理待删除部门（moka_sync_status=3）
     *
     * <p>执行顺序：
     * <ol>
     *     <li>无待删除部门 → 直接跳过</li>
     *     <li>{@code moka.delete-email} 未配置 → 跳过（保留状态 3，不影响 PUT 推送）</li>
     *     <li>孤儿校验：待删部门的子部门中存在「不在删除集合且状态≠4」的 → 跳过该部门</li>
     *     <li>可删部门按树深度自底向上排序（先删子部门再删父部门）</li>
     *     <li>构建删除请求调 Moka DELETE API；成功后逐条 markMokaDeleted（状态 4 + moka_delete_time），
     *     失败保留状态 3 待下轮重试</li>
     * </ol>
     *
     * @return 删除步骤执行结果
     */
    private DeleteOutcome processPendingDeletes() {
        List<MokaDepartmentDO> pendingDeletes = mokaDepartmentService.listByMokaSyncStatus(STATUS_PENDING_DELETE);
        if (pendingDeletes.isEmpty()) {
            log.info("【{}】无待删除部门, 跳过删除步骤", BIZ);
            return new DeleteOutcome(0, 0, null, 0);
        }
        if (!mokaProperties.isDeleteEmailConfigured()) {
            log.info("【{}】待删除部门 {} 条, 但 moka.delete-email 未配置, 跳过删除步骤（保留待删除状态）",
                    BIZ, pendingDeletes.size());
            return new DeleteOutcome(pendingDeletes.size(), 0, null, 0);
        }

        // 孤儿校验：待删部门的子部门中有「不在删除集合且状态≠4」的 → 跳过该部门
        Set<String> deleteCodes = pendingDeletes.stream()
                .map(MokaDepartmentDO::getDepartmentCode)
                .collect(Collectors.toSet());
        List<MokaDepartmentDO> allDepts = mokaDepartmentService.listAll();
        Map<String, List<MokaDepartmentDO>> childrenByParentCode = allDepts.stream()
                .filter(dept -> StringUtils.isNotBlank(dept.getParentCode()))
                .collect(Collectors.groupingBy(MokaDepartmentDO::getParentCode));

        List<MokaDepartmentDO> deletable = new ArrayList<>(pendingDeletes.size());
        int skipped = 0;
        for (MokaDepartmentDO dept : pendingDeletes) {
            List<MokaDepartmentDO> children = childrenByParentCode
                    .getOrDefault(dept.getDepartmentCode(), List.of());
            boolean hasAliveChild = children.stream().anyMatch(child ->
                    !deleteCodes.contains(child.getDepartmentCode())
                            && (child.getMokaSyncStatus() == null
                            || child.getMokaSyncStatus() != STATUS_DELETED));
            if (hasAliveChild) {
                skipped++;
                log.info("【{}】跳过待删除部门（存在未删除的子部门）, departmentCode={}, name={}",
                        BIZ, dept.getDepartmentCode(), dept.getName());
                continue;
            }
            deletable.add(dept);
        }

        if (deletable.isEmpty()) {
            log.info("【{}】待删除部门 {} 条全部被孤儿校验跳过, 未调用 Moka 删除接口",
                    BIZ, pendingDeletes.size());
            return new DeleteOutcome(pendingDeletes.size(), skipped, null, 0);
        }

        // 自底向上排序：深度大的先删，避免父部门先删导致子部门删除失败
        Map<String, Integer> depthCache = new HashMap<>();
        deletable.sort(Comparator.comparingInt(
                        (MokaDepartmentDO dept) -> computeDepth(dept.getDepartmentCode(), depthCache))
                .reversed());

        // 构建删除请求（仅标记删除，不传 target_department_code）
        MokaDeptDeleteRequest request = new MokaDeptDeleteRequest();
        request.setDepartments(deletable.stream().map(dept -> {
            MokaDeptDeleteRequest.MokaDeptDeleteItem item = new MokaDeptDeleteRequest.MokaDeptDeleteItem();
            item.setDepartmentCode(dept.getDepartmentCode());
            return item;
        }).toList());
        request.setEmail(mokaProperties.getDeleteEmail());

        boolean deleteSuccess;
        int deletedCount = 0;
        try {
            log.info("【{}】开始调用 Moka 删除部门, deptCount={}", BIZ, deletable.size());
            mokaOrgService.deleteDepartments(request);
            deleteSuccess = true;

            // 删除成功：逐条标记已删除（状态 4 + moka_delete_time），单条失败不阻断
            for (MokaDepartmentDO dept : deletable) {
                try {
                    deletedCount += mokaDepartmentService.markMokaDeleted(dept.getDepartmentCode());
                } catch (Exception e) {
                    log.error("【{}】标记已删除失败, departmentCode={}", BIZ, dept.getDepartmentCode(), e);
                }
            }
            log.info("【{}】Moka 删除部门成功, deletable={}, markedDeleted={}", BIZ, deletable.size(), deletedCount);

        } catch (BizException | RestClientException e) {
            deleteSuccess = false;
            log.info("【{}】Moka 删除部门终止, reason={}, 待删除部门保留状态 3 待下轮重试", BIZ, e.getMessage());
            log.error("【{}】Moka 删除部门 API 调用失败", BIZ, e);
        }

        return new DeleteOutcome(pendingDeletes.size(), skipped, deleteSuccess, deletedCount);
    }

    /**
     * 计算部门在部门树中的深度（根部门为 1），带缓存与环保护
     *
     * @param departmentCode 部门编码
     * @param depthCache     深度缓存（key=departmentCode, value=深度）；递归中途的临时值 0 兼作环保护标记
     * @return 部门深度；记录不存在或 parent_code 为空/"0" 时返回 1
     */
    private int computeDepth(String departmentCode, Map<String, Integer> depthCache) {
        Integer cached = depthCache.get(departmentCode);
        if (cached != null) {
            return cached;
        }
        // 环保护：先临时标记 0，避免 parent 链成环时无限递归
        depthCache.put(departmentCode, 0);

        MokaDepartmentDO dept = mokaDepartmentService.getByCode(departmentCode);
        int depth;
        if (dept == null || StringUtils.isBlank(dept.getParentCode())
                || ROOT_PARENT_CODE.equals(dept.getParentCode())) {
            depth = 1;
        } else {
            depth = 1 + computeDepth(dept.getParentCode(), depthCache);
        }
        depthCache.put(departmentCode, depth);
        return depth;
    }

    /**
     * 删除步骤执行结果
     *
     * @param pendingCount 待删除（状态 3）部门总数
     * @param skippedCount 孤儿校验跳过的条数
     * @param success      Moka DELETE API 整体是否成功；未调用（无待删/未配置邮箱/全部跳过）时为 null
     * @param deletedCount 删除成功后本地置 4 的条数
     */
    private record DeleteOutcome(int pendingCount, int skippedCount, Boolean success, int deletedCount) {
    }

    // —— DO → API DTO 转换 ——

    /**
     * MokaDepartmentDO 列表 → MokaDepartment（Moka API 请求体元素）列表
     *
     * <p>同时查询子表多语言数据填充 {@code localizedNames}：
     * 每条主表记录查一次子表，N+1 查询但部门量不会太大（百级），可接受。</p>
     */
    private List<MokaDepartment> convertToApiDepartments(List<MokaDepartmentDO> localDepts) {
        List<MokaDepartment> result = new ArrayList<>(localDepts.size());

        for (MokaDepartmentDO dept : localDepts) {
            MokaDepartment apiDept = new MokaDepartment();
            apiDept.setDepartmentCode(dept.getDepartmentCode());
            apiDept.setName(dept.getName());
            apiDept.setParentCode(dept.getParentCode());
            apiDept.setType(dept.getType());

            // sequence: Integer 直接赋值；null 兜底 9999
            apiDept.setSequence(dept.getSequence() != null ? dept.getSequence() : 9999);

            // 多语言子表：查出来填 localizedNames
            List<MokaDepartmentLocalizedDO> localizes =
                    mokaDepartmentService.listLocalizedByDeptCode(dept.getDepartmentCode());
            if (!localizes.isEmpty()) {
                List<MokaDepartment.MokaLocalizedName> names = new ArrayList<>(localizes.size());
                for (MokaDepartmentLocalizedDO loc : localizes) {
                    MokaDepartment.MokaLocalizedName n = new MokaDepartment.MokaLocalizedName();
                    n.setLocale(loc.getLocale());
                    n.setPropValue(loc.getPropValue());
                    names.add(n);
                }
                apiDept.setLocalizedNames(names);
            }

            result.add(apiDept);
        }
        return result;
    }

    /**
     * 批量更新推送部门的 moka_sync_status
     *
     * <p>推送成功走 {@code markMokaSynced}（状态 1 + moka_sync_time），失败走
     * {@code updateMokaSyncStatus(code, 2)}。逐条调用，复用 Service 层事务边界。
     * 百级数据量可接受；若后续上万级再改批量 SQL。</p>
     *
     * @return int[2]: [0]=update 影响行数累加, [1]=DB 异常条数
     */
    private int[] bulkUpdatePushStatus(List<MokaDepartmentDO> depts, boolean mokaSuccess) {
        int updated = 0;
        int failed = 0;
        for (MokaDepartmentDO dept : depts) {
            try {
                if (mokaSuccess) {
                    updated += mokaDepartmentService.markMokaSynced(dept.getDepartmentCode());
                } else {
                    updated += mokaDepartmentService.updateMokaSyncStatus(
                            dept.getDepartmentCode(), STATUS_SYNC_FAILED);
                }
            } catch (Exception e) {
                failed++;
                log.error("【{}】更新同步状态失败, departmentCode={}, targetStatus={}, 已失败={}",
                        BIZ, dept.getDepartmentCode(), mokaSuccess ? STATUS_SYNCED : STATUS_SYNC_FAILED, failed, e);
            }
        }
        return new int[]{updated, failed};
    }
}
