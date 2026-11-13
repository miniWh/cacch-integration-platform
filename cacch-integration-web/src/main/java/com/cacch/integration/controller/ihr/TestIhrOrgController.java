package com.cacch.integration.controller.ihr;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentDetailResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
import com.cacch.integration.service.ihr.api.IIhrOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 测试端点：按部门 ID 查询单个部门详情
     *
     * @param deptId 部门 ID（部门清单 v3 返回的 {@code id} 字段）
     * @return 部门详情完整响应（含 rawJson 原文）；业务失败时由全局异常处理返回错误码
     */
    @GetMapping("/departments/{deptId}/detail")
    public Result<IhrDepartmentDetailResponse> getDepartmentDetail(@PathVariable String deptId) {
        log.info("【IHR组织架构域测试】收到获取部门详情请求, deptId={}", deptId);
        return Result.success(ihrOrgService.getDepartmentDetail(deptId));
    }
}
