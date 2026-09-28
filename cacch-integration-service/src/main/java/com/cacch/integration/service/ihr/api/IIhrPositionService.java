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

    /**
     * 递归查询指定部门及其所有子孙部门下的职位快照
     *
     * <p>内部使用 PostgreSQL WITH RECURSIVE CTE：
     * 以 {@code t_integration_ihr_department} 表构建部门子树，
     * 再关联 {@code t_integration_ihr_position} 表返回匹配职位。
     * 仅返回未逻辑删除的职位，按 position_name 升序排序。</p>
     *
     * @param deptId 起始部门的 iHR 原始 ID（VARCHAR，对应 ihr_dept_id）
     * @return 职位 DO 列表，无数据时返回空列表（非 null）
     */
    List<IhrPositionDO> listByDeptTree(String deptId);
}
