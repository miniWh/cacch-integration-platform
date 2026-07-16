package com.cacch.integration.service.crm.api;

import com.cacch.integration.integration.crm.client.dto.CrmEmployeeQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.crm.client.dto.CrmPageQueryRequest;

/**
 * 勤策 CRM OpenAPI 服务（联调 / 采集底层调用）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmOpenApiService {

    /**
     * 查询订单（透传请求体）
     *
     * @param request 分页查询请求，不可为空
     * @return 勤策响应；return_code 非 0 时抛业务异常
     */
    CrmOpenApiResponse orderQuery(CrmPageQueryRequest request);

    /**
     * 按 modify_time 区间查询订单
     *
     * <p>调用 CRM 时会将时间转为毫秒时间戳，条件为 {@code modify_time GT begin}（可选再加 {@code LT end}）。</p>
     *
     * @param page      页码，从 1 开始；空则默认 1
     * @param rows      每页条数；空则默认 100
     * @param beginTime 起始时间：毫秒时间戳或 yyyy-MM-dd HH:mm:ss，不可为空
     * @param endTime   结束时间：毫秒时间戳或 yyyy-MM-dd HH:mm:ss，可空
     * @return 勤策响应
     */
    CrmOpenApiResponse orderQueryByModifyTime(Integer page, Integer rows, String beginTime, String endTime);

    /**
     * 查询订单明细（透传请求体）
     *
     * @param request 分页查询请求，不可为空
     * @return 勤策响应
     */
    CrmOpenApiResponse orderDetailQuery(CrmPageQueryRequest request);

    /**
     * 按 CRM 订单 ID 查询明细
     *
     * @param page       页码；空则默认 1
     * @param rows       每页条数；空则默认 100
     * @param crmOrderId CRM 订单内部 ID，不可为空
     * @return 勤策响应
     */
    CrmOpenApiResponse orderDetailQueryByOrderId(Integer page, Integer rows, String crmOrderId);

    /**
     * 查询员工帐号
     *
     * @param request 员工查询条件，不可为空
     * @return 勤策响应
     */
    CrmOpenApiResponse queryEmployee(CrmEmployeeQueryRequest request);

    /**
     * 按勤策员工 ID 查询员工帐号
     *
     * @param employeeId 勤策员工唯一标识（对应订单 creator_id.id），不可为空
     * @return 勤策响应
     */
    CrmOpenApiResponse queryEmployeeById(String employeeId);
}
