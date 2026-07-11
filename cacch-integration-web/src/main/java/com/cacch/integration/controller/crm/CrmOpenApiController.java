package com.cacch.integration.controller.crm;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.crm.request.CrmEmployeeByIdRequest;
import com.cacch.integration.dto.crm.request.CrmOrderDetailQueryRequest;
import com.cacch.integration.dto.crm.request.CrmOrderQueryByTimeRequest;
import com.cacch.integration.integration.crm.client.dto.CrmEmployeeQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.crm.client.dto.CrmPageQueryRequest;
import com.cacch.integration.service.crm.api.ICrmOpenApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 勤策 CRM OpenAPI 联调接口（手动触发）
 *
 * <p>凭证取自配置 {@code crm.open-id}/{@code crm.app-key}，调用方无需传密钥。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping("/api/v1/crm/open-api")
@RequiredArgsConstructor
public class CrmOpenApiController {

    private final ICrmOpenApiService crmOpenApiService;

    /**
     * 按 modify_time 区间查询订单（对齐采集默认条件）
     *
     * @param request 时间范围与分页，不可为空
     * @return 勤策原始响应（含 response_data）
     */
    @PostMapping("/orders/query-by-modify-time")
    public Result<CrmOpenApiResponse> queryOrdersByModifyTime(
            @Valid @RequestBody CrmOrderQueryByTimeRequest request) {
        return Result.success(crmOpenApiService.orderQueryByModifyTime(
                request.getPage(), request.getRows(), request.getBeginTime(), request.getEndTime()));
    }

    /**
     * 透传查询订单（完整 page/rows/sorts/query_group）
     *
     * @param request 勤策原始分页查询体，不可为空
     * @return 勤策原始响应
     */
    @PostMapping("/orders/query")
    public Result<CrmOpenApiResponse> queryOrders(@Valid @RequestBody CrmPageQueryRequest request) {
        return Result.success(crmOpenApiService.orderQuery(request));
    }

    /**
     * 按 CRM 订单 ID 查询明细
     *
     * @param request 含 crmOrderId 与分页，不可为空
     * @return 勤策原始响应
     */
    @PostMapping("/order-details/query-by-order-id")
    public Result<CrmOpenApiResponse> queryOrderDetailsByOrderId(
            @Valid @RequestBody CrmOrderDetailQueryRequest request) {
        return Result.success(crmOpenApiService.orderDetailQueryByOrderId(
                request.getPage(), request.getRows(), request.getCrmOrderId()));
    }

    /**
     * 透传查询订单明细
     *
     * @param request 勤策原始分页查询体，不可为空
     * @return 勤策原始响应
     */
    @PostMapping("/order-details/query")
    public Result<CrmOpenApiResponse> queryOrderDetails(@Valid @RequestBody CrmPageQueryRequest request) {
        return Result.success(crmOpenApiService.orderDetailQuery(request));
    }

    /**
     * 按勤策员工 ID 查询员工帐号（取 emp_code）
     *
     * @param request 含 employeeId，不可为空
     * @return 勤策原始响应
     */
    @PostMapping("/employees/query-by-id")
    public Result<CrmOpenApiResponse> queryEmployeeById(@Valid @RequestBody CrmEmployeeByIdRequest request) {
        return Result.success(crmOpenApiService.queryEmployeeById(request.getEmployeeId()));
    }

    /**
     * 透传查询员工帐号
     *
     * @param request 勤策员工查询体，不可为空
     * @return 勤策原始响应
     */
    @PostMapping("/employees/query")
    public Result<CrmOpenApiResponse> queryEmployee(@Valid @RequestBody CrmEmployeeQueryRequest request) {
        return Result.success(crmOpenApiService.queryEmployee(request));
    }
}
