package com.cacch.integration.service.ihr.api;

import com.cacch.integration.entity.ihr.IhrDepartmentDO;

import java.util.List;

/**
 * IHR 部门快照 Service
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrDepartmentService {

    /**
     * 批量 upsert 部门快照
     *
     * @param deptList  部门 DO 列表，不可为空
     * @param syncBatch 本次同步批次号（同一批次内共享，便于追溯）
     * @return upsert 总条数
     */
    int batchUpsert(List<IhrDepartmentDO> deptList, String syncBatch);
}
