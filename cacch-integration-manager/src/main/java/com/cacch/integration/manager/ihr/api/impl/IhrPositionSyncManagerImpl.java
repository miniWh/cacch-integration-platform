package com.cacch.integration.manager.ihr.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.ihr.IhrPositionDO;
import com.cacch.integration.integration.ihr.client.dto.IhrPosition;
import com.cacch.integration.manager.ihr.api.IIhrPositionSyncManager;
import com.cacch.integration.service.ihr.api.IIhrPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * IHR 职位全量同步编排 —— 拉取全量职位 → DTO→DO 映射 → 分批 upsert 到本地快照表
 *
 * <p>调用链：Controller / 定时触发 → syncAll() → IIhrPositionService.fetchAll()
 * → 字段映射 → IIhrPositionService.batchUpsert()</p>
 *
 * <p>事务策略：每次 batchUpsert 独立事务（Service 层内部控制），
 * Manager 层不在外层包裹大事务，避免长事务锁表。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrPositionSyncManagerImpl implements IIhrPositionSyncManager {

    private static final String BIZ = "IHR职位同步编排";

    /**
     * 单次 upsert 批次上限（避免 SQL 过长）
     */
    private static final int BATCH_SIZE = 100;

    private final IIhrPositionService ihrPositionService;

    @Override
    public PositionSyncResult syncAll() {
        String syncBatch = UUID.randomUUID().toString().replace("-", "");
        log.info("【{}】开始全量同步, syncBatch={}", BIZ, syncBatch);

        List<IhrPosition> content;
        try {
            content = ihrPositionService.fetchAll();
        } catch (BizException e) {
            log.info("【{}】IHR 查询终止, reason={}", BIZ, e.getMessage());
            throw e;
        }

        int totalFetched = content == null ? 0 : content.size();
        int upserted = 0;
        int skipped = 0;

        if (totalFetched == 0) {
            log.info("【{}】IHR 返回空, 结束同步", BIZ);
            return new PositionSyncResult(0, 0, 0);
        }

        List<IhrPositionDO> batch = new ArrayList<>(BATCH_SIZE);
        for (IhrPosition ihrPos : content) {
            // uuid 为业务主键，为空则跳过
            if (ihrPos.getId() == null) {
                skipped++;
                log.info("【{}】跳过无 id 的职位, positionName={}", BIZ, ihrPos.getPositionName());
                continue;
            }
            batch.add(mapToDO(ihrPos));

            // 批次满则落库
            if (batch.size() >= BATCH_SIZE) {
                upserted += ihrPositionService.batchUpsert(batch, syncBatch);
                log.info("【{}】批次落库完成, batchSize={}, cumulativeUpserted={}",
                        BIZ, batch.size(), upserted);
                batch.clear();
            }
        }

        // 最后剩余落库
        if (!batch.isEmpty()) {
            upserted += ihrPositionService.batchUpsert(batch, syncBatch);
            log.info("【{}】尾批次落库完成, batchSize={}, cumulativeUpserted={}", BIZ, batch.size(), upserted);
        }

        log.info("【{}】同步完成, totalFetched={}, upserted={}, skipped={}, syncBatch={}",
                BIZ, totalFetched, upserted, skipped, syncBatch);

        return new PositionSyncResult(totalFetched, upserted, skipped);
    }

    /**
     * IhrPosition（IHR DTO） → IhrPositionDO（本地快照表）字段映射
     *
     * <p>关键转换：
     * <ul>
     *     <li>{@code id}（Long）→ {@code uuid}（String 业务主键）</li>
     *     <li>{@code companyId / departmentId / parentId}（Long）→ 对应 DO 字段（String）</li>
     *     <li>日期字段（affectiveDate 等）保持原始 String，不做类型转换</li>
     * </ul>
     */
    private IhrPositionDO mapToDO(IhrPosition src) {
        IhrPositionDO DO = new IhrPositionDO();
        DO.setUuid(src.getId() != null ? String.valueOf(src.getId()) : null);
        DO.setCompanyId(src.getCompanyId() != null ? String.valueOf(src.getCompanyId()) : null);
        DO.setPositionName(src.getPositionName());
        DO.setAbbreviation(src.getAbbreviation());
        DO.setPositionCode(src.getPositionCode());
        DO.setApplyRange(src.getApplyRange());
        DO.setCapacity(src.getCapacity());
        DO.setAffectiveDate(src.getAffectiveDate());
        DO.setExpiryDate(src.getExpiryDate());
        DO.setPositionScope(src.getPositionScope());
        DO.setDescription(src.getDescription());
        DO.setDepartmentId(src.getDepartmentId() != null ? String.valueOf(src.getDepartmentId()) : null);
        DO.setDepartmentName(src.getDepartmentName());
        DO.setJobFunction(src.getJobFunction());
        DO.setJobSubFunction(src.getJobSubFunction());
        DO.setPositionGraded(src.getPositionGraded());
        DO.setPositionGradeName(src.getPositionGradeName());
        DO.setQualification(src.getQualification());
        DO.setParentId(src.getParentId() != null ? String.valueOf(src.getParentId()) : null);
        DO.setIsPositionGroup(src.getIsPositionGroup());
        DO.setPositionState(src.getPositionState());
        DO.setPositionStateString(src.getPositionStateString());
        DO.setUpdateDate(src.getUpdateDate());
        DO.setCreateDate(src.getCreateDate());
        return DO;
    }
}
