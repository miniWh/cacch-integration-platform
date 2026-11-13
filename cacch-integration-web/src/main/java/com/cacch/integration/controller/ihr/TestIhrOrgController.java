package com.cacch.integration.controller.ihr;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentDetailResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
import com.cacch.integration.service.ihr.api.IIhrOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * IHR 组织架构/部门域测试端点 — 部门类所有接口测试端点的聚合 Controller
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/test/ihr/org")
@RequiredArgsConstructor
public class TestIhrOrgController {

    private final IIhrOrgService ihrOrgService;

    /**
     * 测试端点：分页查询部门清单（获取部门清单 v3）— 单页透传
     *
     * @param request 分页与搜索条件；{@code page} 从 0 开始，
     *                {@code size} 未传或 ≤0 时端点内默认 20
     * @return 部门清单原始响应（含分页元信息 totalElements / totalPages / end）
     */
    @PostMapping("/departments/search")
    public Result<IhrOrgSearchResponse> searchDepartments(@RequestBody IhrOrgSearchRequest request) {
        if (request.getSize() <= 0) {
            request.setSize(20);
        }
        log.info("【IHR组织架构域测试】收到部门清单查询请求, page={}, size={}", request.getPage(), request.getSize());
        return Result.success(ihrOrgService.searchDepartments(request));
    }

    /**
     * 测试端点：按部门编码查询单个部门详情
     *
     * @param departmentCode 部门编码
     * @return 部门详情完整响应（含 rawJson 原文）；业务失败时由全局异常处理返回错误码
     */
    @GetMapping("/departments/{departmentCode}/detail")
    public Result<IhrDepartmentDetailResponse> getDepartmentDetail(@PathVariable String departmentCode) {
        log.info("【IHR组织架构域测试】收到获取部门详情请求, departmentCode={}", departmentCode);
        return Result.success(ihrOrgService.getDepartmentDetail(departmentCode));
    }

    /**
     * 测试端点：根据部门 id 集合批量查询部门详情（联调用）
     *
     * <p>用于实测该接口的返回字段全集（重点确认 sequence / lastUpdate / createdDate /
     * costName 等新增字段是否返回，未返回字段显示为 null）；
     * 注意返回结果已经过 parentId=-1 → null 归一化。</p>
     *
     * @param departmentIds 部门 ID 数组（List&lt;Long&gt;），非空
     * @return 部门详情列表（iHR 侧不存在的 ID 无对应元素）；业务失败时由全局异常处理返回错误码
     */
    @PostMapping("/departments/list-by-ids")
    public Result<List<IhrDepartment>> listDepartmentsByIds(@RequestBody List<Long> departmentIds) {
        if (CollectionUtils.isEmpty(departmentIds)) {
            log.info("【IHR组织架构域测试】按ID集合查询部门终止, reason=departmentIds 为空");
            return Result.success(List.of());
        }
        log.info("【IHR组织架构域测试】收到按ID集合查询部门请求, deptIdCount={}, deptIds={}",
                departmentIds.size(), departmentIds);
        return Result.success(ihrOrgService.listDepartmentsByIds(departmentIds));
    }
}
