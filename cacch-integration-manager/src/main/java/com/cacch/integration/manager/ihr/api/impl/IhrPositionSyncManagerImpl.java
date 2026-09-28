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

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

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
     * <p>2026-09-28 在线文档确认的关键转换：
     * <ul>
     *     <li>{@code id} 本身就是 String UUID — 直接赋值 DO.uuid</li>
     *     <li>{@code companyId / parentId} 都是 String UUID — 直接赋值</li>
     *     <li>{@code positionState} 为 String（ENABLE / DISABLE）— 直接赋值</li>
     *     <li>{@code departmentId}（Long）→ DO.departmentId（String）</li>
     *     <li>{@code positionScope}（List<Integer>）→ JSON 字符串存入 DO.positionScope（对应 PG JSONB）</li>
     * </ul>
     */
    private IhrPositionDO mapToDO(IhrPosition src) {
        IhrPositionDO DO = new IhrPositionDO();
        DO.setUuid(src.getId());                           // String UUID 直接赋值
        DO.setCompanyId(src.getCompanyId());               // String UUID 直接赋值
        DO.setPositionName(src.getPositionName());
        DO.setAbbreviation(src.getAbbreviation());
        DO.setPositionCode(src.getPositionCode());
        DO.setAppliedRange(src.getAppliedRange());
        DO.setCapacity(src.getCapacity());
        DO.setEffectiveDate(src.getEffectiveDate());
        DO.setDescription(src.getDescription());
        DO.setDepartmentId(src.getDepartmentId() != null ? String.valueOf(src.getDepartmentId()) : null);
        DO.setDepartmentName(src.getDepartmentName());
        DO.setJobTitleId(src.getJobTitleId());
        DO.setJobTitleName(src.getJobTitleName());
        DO.setPositionGradeId(src.getPositionGradeId());
        DO.setPositionGradeName(src.getPositionGradeName());
        DO.setQualifications(src.getQualifications());
        DO.setParentId(src.getParentId());                 // String UUID 直接赋值
        DO.setIsPositionGroup(src.getIsPositionGroup());
        DO.setPositionState(src.getPositionState());       // ENABLE/DISABLE String 直接赋值
        DO.setPositionStateString(src.getPositionStateString());
        DO.setUpdatedDate(src.getUpdatedDate());
        DO.setCreatedDate(src.getCreatedDate());
        DO.setPositionScope(toJson(src.getPositionScope()));
        return DO;
    }

    /**
     * List<Integer> → JSON 字符串（PG JSONB 列）；null / 空列表返回 null
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.info("【{}】positionScope JSON 序列化终止, reason={}", BIZ, e.getMessage());
            return null;
        }
    }
}
