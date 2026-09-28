package com.cacch.integration.service.ihr.api;

import com.cacch.integration.entity.ihr.IhrPositionDO;
import com.cacch.integration.integration.ihr.client.dto.IhrPosition;

import java.util.List;

/**
 * IHR 职位快照 Service
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrPositionService {

    /**
     * 从 IHR 全量拉取职位清单
     *
     * <p>接口无分页，一次返回全部 resultArray。若收到 401/403 或业务 code 非 200，
     * 会强制刷新 token 后重试一次。</p>
     *
     * @return iHR 原始职位 DTO 列表，无数据时返回空列表（非 null）
     */
    List<IhrPosition> fetchAll();

    /**
     * 批量 upsert 职位快照 — 单批次内开启事务，中途失败回滚全部
     *
     * @param positionList 职位 DO 列表，不可为空
     * @param syncBatch    本次同步批次号（同一批次内共享，便于追溯）
     * @return upsert 总条数
     */
    int batchUpsert(List<IhrPositionDO> positionList, String syncBatch);
}
