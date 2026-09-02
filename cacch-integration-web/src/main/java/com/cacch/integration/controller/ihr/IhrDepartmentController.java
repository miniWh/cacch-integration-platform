package com.cacch.integration.controller.ihr;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.ihr.IhrOrgConverter;
import com.cacch.integration.dto.ihr.request.SearchDepartmentRequest;
import com.cacch.integration.dto.ihr.vo.DepartmentPageVO;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.manager.ihr.api.IIhrOrgManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IHR 开放平台部门查询 REST 接口
 *
 * <p>鉴权：IHR 凭证由配置文件 {@code ihr.app-key} / {@code ihr.app-secret}（经环境变量 IHR_APP_KEY / IHR_APP_SECRET 注入）注入，
 * 调用方无需传递密钥。内部已封装 access_token 缓存与自动续期。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/ihr/departments")
@RequiredArgsConstructor
public class IhrDepartmentController {

    private final IIhrOrgManager ihrOrgManager;
    private final IhrOrgConverter ihrOrgConverter;

    /**
     * 分页查询 IHR 部门清单
     *
     * <p>调用方可在 {@code conditions} 中传入 0..N 个搜索条件；空集合视为全量查询。
     * 典型搜索字段：{@code departmentName} / {@code departmentCode} / {@code departmentId}，
     * 搜索类型：{@code EQUAL} / {@code LIKE} / {@code FUZZY} / {@code IN}。</p>
     *
     * @param request 分页与搜索条件，不可为空
     * @return 部门分页视图（含 departments / totalElements / totalPages / end）
     */
    @PostMapping("/search")
    public Result<DepartmentPageVO> searchDepartments(@Valid @RequestBody SearchDepartmentRequest request) {
        log.info("收到部门查询请求, page={}, size={}", request.getPage(), request.getSize());
        IhrOrgSearchRequest upstreamRequest = ihrOrgConverter.toUpstreamRequest(request);
        return Result.success(ihrOrgConverter.toPageVO(ihrOrgManager.searchDepartments(upstreamRequest)));
    }
}
