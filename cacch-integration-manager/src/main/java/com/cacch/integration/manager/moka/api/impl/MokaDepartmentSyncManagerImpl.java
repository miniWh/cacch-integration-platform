package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.moka.MokaDepartmentDO;
import com.cacch.integration.entity.moka.MokaDepartmentLocalizedDO;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
import com.cacch.integration.manager.ihr.api.IIhrOrgManager;
import com.cacch.integration.manager.moka.api.IMokaDepartmentSyncManager;
import com.cacch.integration.service.moka.api.IMokaDepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Moka 部门同步编排实现 — 从 IHR 翻页拉取全量部门，映射后批量 upsert 到 Moka 表
 *
 * <p>调用链：Controller → syncFromIhr() → IIhrOrgManager.searchDepartments()（循环翻页）
 * → 字段映射 → IMokaDepartmentService.batchUpsert()（主表）+ batchUpsertLocalized()（子表）</p>
 *
 * <p>事务策略：每次批量 upsert 独立事务（Propagation.REQUIRES_NEW 由 Service 内部控制），
 * Manager 层不在外层包裹大事务，避免长事务锁表。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaDepartmentSyncManagerImpl implements IMokaDepartmentSyncManager {

    private static final String BIZ = "Moka部门同步编排";

    /**
     * 每次从 IHR 拉取的条数（分页 size）
     */
    private static final int PAGE_SIZE = 100;

    /**
     * 子表 locale 固定值
     */
    private static final String LOCALE_ZH_CN = "zh_CN";

    /**
     * iHR 根部门 parent_code 兜底值
     */
    private static final String ROOT_PARENT_CODE = "0";

    private final IIhrOrgManager ihrOrgManager;
    private final IMokaDepartmentService mokaDepartmentService;

    @Override
    public MokaDeptSyncResult syncFromIhr() {
        log.info("【{}】开始从 IHR 全量同步部门到 Moka 表, pageSize={}", BIZ, PAGE_SIZE);

        int totalFetched = 0;
        int deptSkipped = 0;

        // 逐页拉取 IHR 数据，每次收集一批（PAGE_SIZE * BATCH_PAGES）后批量 upsert
        // 这里简化为每 PAGE_SIZE 条一批，避免单次 upsert 列表过大
        List<MokaDepartmentDO> deptBatch = new ArrayList<>(PAGE_SIZE);
        List<MokaDepartmentLocalizedDO> localBatch = new ArrayList<>(PAGE_SIZE);

        int page = 0;
        boolean end = false;
        while (!end) {
            IhrOrgSearchRequest request = new IhrOrgSearchRequest();
            request.setPage(page);
            request.setSize(PAGE_SIZE);
            // searchArgsList 留空 = 全量查询

            IhrOrgSearchResponse response;
            try {
                response = ihrOrgManager.searchDepartments(request);
            } catch (BizException e) {
                log.info("【{}】IHR 查询终止, page={}, reason={}", BIZ, page, e.getMessage());
                throw e;
            }

            List<IhrDepartment> content = response.getData();
            if (content == null || content.isEmpty()) {
                log.info("【{}】IHR 返回空页, page={}, 结束拉取", BIZ, page);
                break;
            }

            totalFetched += content.size();
            log.info("【{}】IHR 分页拉取, page={}, currentSize={}, totalFetched={}, ihrEnd={}",
                    BIZ, page, content.size(), totalFetched, response.getEnd());

            // 逐条转换并加入批次
            for (IhrDepartment ihrDept : content) {
                // 校验：departmentCode 为空则跳过
                if (ihrDept.getDepartmentCode() == null || ihrDept.getDepartmentCode().isBlank()) {
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

            // 翻页终止条件
            end = Boolean.TRUE.equals(response.getEnd());
            page++;
        }

        if (deptBatch.isEmpty()) {
            log.info("【{}】IHR 未拉取到有效部门, 同步结束", BIZ);
            return new MokaDeptSyncResult(totalFetched, 0, 0, deptSkipped);
        }

        // 批量落库
        log.info("【{}】开始批量落库, totalFetched={}, validToUpsert={}, skipped={}",
                BIZ, totalFetched, deptBatch.size(), deptSkipped);

        int deptUpserted = mokaDepartmentService.batchUpsert(deptBatch);
        int localizedUpserted = mokaDepartmentService.batchUpsertLocalized(localBatch);

        log.info("【{}】同步完成, totalFetched={}, deptUpserted={}, localizedUpserted={}, deptSkipped={}",
                BIZ, totalFetched, deptUpserted, localizedUpserted, deptSkipped);

        return new MokaDeptSyncResult(totalFetched, deptUpserted, localizedUpserted, deptSkipped);
    }

    // —— 字段映射 ——

    /**
     * IhrDepartment → MokaDepartmentDO 字段映射
     *
     * <p>映射规则：
     * <ul>
     *     <li>departmentCode → department_code（主键）</li>
     *     <li>name → name</li>
     *     <li>parentDepartmentCode → parent_code（null/空兜底 "0"）</li>
     *     <li>type（COMPANY/DEPARTMENT/STORE）→ type（1/1/2）</li>
     *     <li>sequence → sequence（Long → BigDecimal）</li>
     * </ul>
     */
    private MokaDepartmentDO mapMainTable(IhrDepartment ihr) {
        MokaDepartmentDO moka = new MokaDepartmentDO();

        moka.setDepartmentCode(ihr.getDepartmentCode());
        moka.setName(ihr.getName());

        // parent_code：parentDepartmentCode 为 null/空 时兜底 "0"（根部门）
        String parentDeptCode = ihr.getParentDepartmentCode();
        moka.setParentCode((parentDeptCode == null || parentDeptCode.isBlank())
                ? ROOT_PARENT_CODE
                : parentDeptCode);

        // type 枚举转换
        moka.setType(convertType(ihr.getType()));

        // sequence：Long → BigDecimal
        if (ihr.getSequence() != null) {
            moka.setSequence(BigDecimal.valueOf(ihr.getSequence().longValue()));
        }

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
