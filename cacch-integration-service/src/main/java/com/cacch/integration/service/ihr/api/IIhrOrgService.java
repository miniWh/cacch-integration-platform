package com.cacch.integration.service.ihr.api;

import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentDetailResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;

import java.util.List;

/**
 * IHR 组织架构/部门服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrOrgService {

    /**
     * 条件查询部门
     *
     * @param request 分页与搜索条件
     * @return IHR 部门分页结果
     */
    IhrOrgSearchResponse searchDepartments(IhrOrgSearchRequest request);

    /**
     * 全量拉取 iHR 部门清单（获取部门清单 v3，分页翻页至末页）
     *
     * <p>按 {@code page} 从 0 起、每页 100 条、无搜索条件（全量）循环拉取，
     * 以 {@code end=true} 或 {@code page 达到 totalPages-1} 终止；
     * 每页校验业务 code（非 0 抛 BizException），鉴权失败自动强刷 token 重试
     * （复用 {@link #searchDepartments} 内部重试逻辑）。首页即空时返回空列表。</p>
     *
     * @return iHR 全量部门列表（含 uuid / departmentCode / parentDepartmentCode 等）；无数据时返回空列表（非 null）
     * @throws com.cacch.integration.common.exception.BizException 任一页 HTTP 调用失败（含重试后）或业务 code 非 0 时抛出（整轮终止）
     */
    List<IhrDepartment> fetchAllDepartments();

    /**
     * 根据部门 ID 集合批量获取部门详情（自动分批，每批 100）
     *
     * <p>内部按 100 个 ID 一批调用 iHR「根据部门id获取部门详情」接口
     * （POST organizations/list，Body 为 ID 数组），汇总全部批次结果返回；
     * 每批校验业务 code（非 0 抛 BizException），鉴权失败自动强刷 token 重试。
     * 返回前对 parentId 归一化：list 接口以 -1 表示无上级部门，统一转 null
     * （与搜索接口"无上级不返回该字段"语义对齐，保证 rowHash 跨接口稳定）。</p>
     *
     * @param departmentIds 部门 ID 集合；null 或空时返回空列表
     * @return 部门详情列表（iHR 侧不存在的 ID 无对应元素）；无数据时返回空列表（非 null）
     * @throws com.cacch.integration.common.exception.BizException 任一批次 HTTP 调用失败（含重试后）或业务 code 非 0 时抛出
     */
    List<IhrDepartment> listDepartmentsByIds(List<Long> departmentIds);

    /**
     * 查询单个部门详情（按部门编码）
     *
     * @param departmentCode 部门编码
     * @return 部门详情响应（含 rawJson 原文，供贴源留底）
     * @throws com.cacch.integration.common.exception.BizException departmentCode 为空、HTTP 调用失败（含重试后）或业务 code 非 0 时抛出
     */
    IhrDepartmentDetailResponse getDepartmentDetail(String departmentCode);
}
