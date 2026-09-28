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

    /**
     * 递归查询指定 parent_id 下的所有子孙部门（仅 ENABLE 状态、未逻辑删除）
     *
     * <p>内部使用 PostgreSQL WITH RECURSIVE CTE 递归遍历部门树。
     * 锚点条件为 {@code parent_id = 指定值}，递归时子记录的 parent_id
     * 关联父记录的 ihr_dept_id（iHR 原始部门 ID）。结果按 sequence 升序、
     * id 升序稳定排序，无数据时返回空列表。</p>
     *
     * @param parentId 父部门的 iHR 原始 ID（对应表字段 parent_id），如 {@code "396"}
     * @return 子孙部门 DO 列表，不会返回 null
     */
    List<IhrDepartmentDO> listRecursiveByParentId(String parentId);

    /**
     * 按 ihr_dept_id 批量查询部门快照
     *
     * <p>用于 Moka 人员同步时，将 persondetail.departmentId 批量映射为
     * ihr_department.department_code。传入空列表时直接返回空列表，
     * 不会查询 DB。</p>
     *
     * @param ihrDeptIds iHR 部门 ID 列表（persondetail.departmentId）；null 或空时返回空列表
     * @return 匹配的部门 DO 列表，不会返回 null
     */
    List<IhrDepartmentDO> listByIhrDeptIds(List<String> ihrDeptIds);
}
