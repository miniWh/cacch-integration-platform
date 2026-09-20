package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.moka.MokaDepartmentDO;
import com.cacch.integration.entity.moka.MokaDepartmentLocalizedDO;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDepartment;
import com.cacch.integration.manager.moka.api.IMokaDepartmentPushManager;
import com.cacch.integration.service.moka.api.IMokaDepartmentService;
import com.cacch.integration.service.moka.api.IMokaOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * Moka 部门推送实现 —— 本地 PG 表 → Moka 开放平台 API
 *
 * <p>全量推送语义：Moka PUT /api-platform/v2/departments 以 departmentCode 为主键，
 * 本次传入的 departments 即 Moka 侧完整最新列表。因此推送范围必须是本地 PG 全量，
 * 不能只推 PENDING/FAILED 的子集（否则 Moka 会把未传入的已有部门标记删除）。</p>
 *
 * <p>moka_sync_status 更新策略：
 * <ul>
 *     <li>Moka API 返回成功 → 全量 updateMokaSyncStatus(1, SYNCED)</li>
 *     <li>Moka API 调用异常 / code≠0 → 全量 updateMokaSyncStatus(2, SYNC_FAILED)</li>
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
     * 操作人邮箱默认值（Moka 侧日志记录用）
     */
    private static final String DEFAULT_OPERATOR_EMAIL = "system@cacch.com";

    /**
     * moka_sync_status 常量
     */
    private static final int STATUS_SYNCED = 1;
    private static final int STATUS_SYNC_FAILED = 2;

    private final IMokaDepartmentService mokaDepartmentService;
    private final IMokaOrgService mokaOrgService;

    @Override
    public MokaDeptPushResult pushToMoka(String operatorEmail) {
        // 1) 查询本地 PG 全量部门
        List<MokaDepartmentDO> localDepts = mokaDepartmentService.listAll();
        if (localDepts.isEmpty()) {
            log.info("【{}】本地 PG 无部门数据，跳过推送", BIZ);
            return new MokaDeptPushResult(0, 0, null, null, null, 0);
        }
        log.info("【{}】查询本地 PG 部门全量, count={}", BIZ, localDepts.size());

        // 2) 转换为 Moka API 请求体（含多语言子表数据）
        List<MokaDepartment> apiDepts = convertToApiDepartments(localDepts);
        MokaDeptSyncRequest request = new MokaDeptSyncRequest();
        request.setDepartments(apiDepts);
        request.setOperatorEmail((operatorEmail != null && !operatorEmail.isBlank())
                ? operatorEmail
                : DEFAULT_OPERATOR_EMAIL);

        // 3) 调用 Moka 开放平台全量同步 API
        MokaDeptSyncResponse response;
        boolean mokaSuccess = false;
        Integer newCount = null;
        Integer updateCount = null;
        Integer deleteCount = null;

        try {
            log.info("【{}】开始推送 Moka, deptCount={}, operatorEmail={}",
                    BIZ, apiDepts.size(), request.getOperatorEmail());
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

        // 4) 更新本地 moka_sync_status
        int statusToSet = mokaSuccess ? STATUS_SYNCED : STATUS_SYNC_FAILED;
        int updated = bulkUpdateSyncStatus(localDepts, statusToSet);
        log.info("【{}】更新本地同步状态为 {}, 更新条数={}", BIZ, statusToSet, updated);

        return new MokaDeptPushResult(
                localDepts.size(),
                localDepts.size(),
                newCount, updateCount, deleteCount,
                updated
        );
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
     * 批量更新部门列表的 moka_sync_status
     *
     * <p>逐条调用 updateMokaSyncStatus，复用 Service 层事务边界。
     * 百级数据量可接受；若后续上万级再改批量 SQL。</p>
     */
    private int bulkUpdateSyncStatus(List<MokaDepartmentDO> depts, int targetStatus) {
        int updated = 0;
        int failed = 0;
        for (MokaDepartmentDO dept : depts) {
            try {
                updated += mokaDepartmentService.updateMokaSyncStatus(
                        dept.getDepartmentCode(), targetStatus);
            } catch (Exception e) {
                failed++;
                log.error("【{}】更新同步状态失败, departmentCode={}, targetStatus={}, 已失败={}",
                        BIZ, dept.getDepartmentCode(), targetStatus, failed, e);
            }
        }
        return updated;
    }
}
