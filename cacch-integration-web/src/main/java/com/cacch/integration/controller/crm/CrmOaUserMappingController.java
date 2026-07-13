package com.cacch.integration.controller.crm;

import com.cacch.integration.common.dto.crm.CrmOaUserMappingResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.crm.request.CrmOaUserMappingResolveRequest;
import com.cacch.integration.entity.crm.CrmOaUserMappingDO;
import com.cacch.integration.manager.crm.api.ICrmOaUserMappingManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM↔OA 人员映射联调接口
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping("/api/v1/crm/oa-user-mapping")
@RequiredArgsConstructor
public class CrmOaUserMappingController {

    private final ICrmOaUserMappingManager crmOaUserMappingManager;

    /**
     * 解析 CRM 员工 → OA 人员（优先读库，未命中则调 CRM/OA 并落库）
     *
     * @param request 含 crmEmployeeId、可选 forceRefresh
     * @return 映射结果（失败时 success=false，HTTP 仍为业务成功包装）
     */
    @PostMapping("/resolve")
    public Result<CrmOaUserMappingResult> resolve(@Valid @RequestBody CrmOaUserMappingResolveRequest request) {
        boolean force = Boolean.TRUE.equals(request.getForceRefresh());
        return Result.success(crmOaUserMappingManager.resolve(request.getCrmEmployeeId(), force));
    }

    /**
     * 仅查询库表缓存（不调三方）
     *
     * @param crmEmployeeId CRM 员工 ID
     * @return 映射记录；不存在时 data 为 null
     */
    @GetMapping("/{crmEmployeeId}")
    public Result<CrmOaUserMappingDO> getCached(@PathVariable String crmEmployeeId) {
        return Result.success(crmOaUserMappingManager.getCached(crmEmployeeId));
    }
}
