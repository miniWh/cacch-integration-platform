package com.cacch.integration.controller.ihr;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentDetailResponse;
import com.cacch.integration.service.ihr.api.IIhrOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
