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

    /**
     * 查询所有启用状态（department_status = 'ENABLE'）的部门快照
     *
     * <p>用于下游（如 Moka 部门同步）从本地快照表读取 IHR 部门数据，
     * 避免每次同步都回源调用 IHR 开放平台接口。结果仅含未逻辑删除记录，
     * 按 sequence 升序、id 升序稳定排序，无数据时返回空列表。</p>
     *
     * @return 启用状态的部门 DO 列表，不会返回 null
     */
    List<IhrDepartmentDO> listEnabled();
}
