package com.cacch.integration.manager.ihr.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.ihr.IhrDepartmentDO;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
import com.cacch.integration.manager.ihr.api.IIhrDeptSyncManager;
import com.cacch.integration.manager.ihr.api.IIhrOrgManager;
import com.cacch.integration.service.ihr.api.IIhrDepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * IHR 部门全量同步编排 —— 翻页拉取全量部门 → DTO→DO 映射 → 批量 upsert 到本地快照表
 *
 * <p>调用链：Controller / 定时触发 → syncAll() → IIhrOrgManager.searchDepartments()（循环翻页）
 * → 字段映射 → IIhrDepartmentService.batchUpsert()</p>
 *
 * <p>事务策略：每次批量 upsert 独立事务（Service 层内部控制），
 * Manager 层不在外层包裹大事务，避免长事务锁表。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrDeptSyncManagerImpl implements IIhrDeptSyncManager {

    private static final String BIZ = "IHR部门同步编排";

    /**
     * 每次从 IHR 拉取的条数（分页 size，IHR 上限 200）
     */
    private static final int PAGE_SIZE = 100;

    /**
     * 单次 upsert 批次上限（避免 SQL 过长）
     */
    private static final int BATCH_SIZE = 100;

    private final IIhrOrgManager ihrOrgManager;
    private final IIhrDepartmentService ihrDepartmentService;

    @Override
    public IhrDeptSyncResult syncAll() {
        String syncBatch = UUID.randomUUID().toString().replace("-", "");
        log.info("【{}】开始全量同步, pageSize={}, syncBatch={}", BIZ, PAGE_SIZE, syncBatch);

        int totalFetched = 0;
        int upserted = 0;
        int skipped = 0;
        List<IhrDepartmentDO> batch = new ArrayList<>(BATCH_SIZE);

        int page = 0;
        boolean end = false;
        while (!end) {
            IhrOrgSearchRequest request = new IhrOrgSearchRequest();
            request.setPage(page);
            request.setSize(PAGE_SIZE);

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
            log.info("【{}】分页拉取, page={}, currentSize={}, totalFetched={}, ihrEnd={}",
                    BIZ, page, content.size(), totalFetched, response.getEnd());

            for (IhrDepartment ihrDept : content) {
                // uuid 为业务主键，为空则跳过
                if (ihrDept.getUuid() == null || ihrDept.getUuid().isBlank()) {
                    skipped++;
                    log.info("【{}】跳过无 uuid 的部门, name={}", BIZ, ihrDept.getName());
                    continue;
                }
                batch.add(mapToDO(ihrDept));
            }

            // 批次满则落库
            if (batch.size() >= BATCH_SIZE) {
                upserted += ihrDepartmentService.batchUpsert(batch, syncBatch);
                log.info("【{}】批次落库完成, batchSize={}, cumulativeUpserted={}",
                        BIZ, batch.size(), upserted);
                batch.clear();
            }

            end = Boolean.TRUE.equals(response.getEnd());
            page++;
        }

        // 最后剩余落库
        if (!batch.isEmpty()) {
            upserted += ihrDepartmentService.batchUpsert(batch, syncBatch);
            log.info("【{}】尾批次落库完成, batchSize={}, cumulativeUpserted={}", BIZ, batch.size(), upserted);
        }

        log.info("【{}】同步完成, totalFetched={}, upserted={}, skipped={}, syncBatch={}",
                BIZ, totalFetched, upserted, skipped, syncBatch);

        return new IhrDeptSyncResult(totalFetched, upserted, skipped);
    }

    /**
     * IhrDepartment（IHR DTO） → IhrDepartmentDO（本地快照表）字段映射
     *
     * <p>关键转换：
     * <ul>
     *     <li>{@code id}（Long）→ {@code ihrDeptId}（String）</li>
     *     <li>{@code parentId}（Long）→ {@code parentId}（String）</li>
     *     <li>{@code createdDate}（Long 毫秒）→ {@code createdDate}（LocalDateTime，显式转换）</li>
     * </ul>
     */
    private IhrDepartmentDO mapToDO(IhrDepartment src) {
        IhrDepartmentDO DO = new IhrDepartmentDO();
        DO.setUuid(src.getUuid());
        DO.setIhrDeptId(src.getId() != null ? String.valueOf(src.getId()) : null);
        DO.setName(src.getName());
        DO.setParentId(src.getParentId() != null ? String.valueOf(src.getParentId()) : null);
        DO.setType(src.getType());
        DO.setDepartmentCode(src.getDepartmentCode());
        DO.setStoreNumber(src.getStoreNumber());
        DO.setPrincipalStaffId(src.getPrincipalStaffId());
        DO.setParentDepartmentCode(src.getParentDepartmentCode());
        DO.setParentDepartmentName(src.getParentDepartmentName());
        DO.setVirtual(src.getVirtual());
        DO.setDepartmentStatus(src.getDepartmentStatus());
        DO.setDepartmentDesc(src.getDepartmentDesc());
        DO.setDepartmentProperty(src.getDepartmentProperty());
        DO.setLastUpdate(src.getLastUpdate());
        DO.setCreatedDate(toLocalDateTime(src.getCreatedDate()));
        DO.setAbbreviation(src.getAbbreviation());
        DO.setEstablishDate(src.getEstablishDate());
        DO.setEffectiveDate(src.getEffectiveDate());
        DO.setRemark(src.getRemark());
        DO.setSequence(src.getSequence());
        return DO;
    }

    /**
     * iHR 毫秒时间戳 → LocalDateTime；null / 非正数返回 null
     */
    private LocalDateTime toLocalDateTime(Long millis) {
        if (millis == null || millis <= 0) {
            return null;
        }
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
