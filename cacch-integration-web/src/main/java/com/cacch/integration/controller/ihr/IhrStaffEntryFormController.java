package com.cacch.integration.controller.ihr;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.ihr.IhrStaffEntryFormConverter;
import com.cacch.integration.dto.ihr.request.IhrStaffEntryFormCreateApiRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffEntryFormCreateResponse;
import com.cacch.integration.service.ihr.api.IIhrStaffEntryFormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IHR 待入职 REST 接口
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/ihr/entry-forms")
@RequiredArgsConstructor
public class IhrStaffEntryFormController {

    private final IIhrStaffEntryFormService ihrStaffEntryFormService;
    private final IhrStaffEntryFormConverter converter;

    /**
     * 新增待入职-简略
     *
     * <p>快速将信息较少的员工添加到 iHR 待入职列表。
     * 调用 IHR 接口：POST /openapi/thirdparty/api/staff/v1/entryForms/create
     * 本接口为测试透传入口，落库不在范围内。</p>
     *
     * <p>必填字段：staffName / mobile / expectEntryDate / departmentId。
     * HTTP 401/403 时 Service 层自动强制刷新 token 后重试一次。</p>
     *
     * @param request 待入职请求体（jakarta.validation 注解校验，非法时 400 参数错误）
     * @return iHR 原始响应（含 entryId、data、errorResult）
     */
    @PostMapping("/create")
    public Result<IhrStaffEntryFormCreateResponse> create(@Valid @RequestBody IhrStaffEntryFormCreateApiRequest request) {
        log.info("收到 IHR 待入职创建请求, staffName={}, departmentId={}",
                request.getStaffName(), request.getDepartmentId());
        IhrStaffEntryFormCreateResponse response = ihrStaffEntryFormService.createEntryForm(
                converter.toIntegrationRequest(request));
        return Result.success(response);
    }
}
