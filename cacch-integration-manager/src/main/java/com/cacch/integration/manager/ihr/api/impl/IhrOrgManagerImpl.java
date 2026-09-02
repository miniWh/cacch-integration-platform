package com.cacch.integration.manager.ihr.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
import com.cacch.integration.manager.ihr.api.IIhrOrgManager;
import com.cacch.integration.service.ihr.api.IIhrOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * IHR 部门查询编排实现 — 调用 Service 完成 token 获取 + 上游查询，异常统一包装为 BizException
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrOrgManagerImpl implements IIhrOrgManager {

    private final IIhrOrgService ihrOrgService;

    private static final String BIZ = "IHR 组织架构/部门查询编排实现";

    @Override
    public IhrOrgSearchResponse searchDepartments(IhrOrgSearchRequest request) {
        if (request == null) {
            log.info("【{}】查询部门终止, reason=请求体为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "IHR 部门查询请求体为空");
        }

        log.info("【{}】开始查询部门, page={}, size={}, conditions={}",
                BIZ, request.getPage(), request.getSize(), request.getSearchArgsList());

        try {
            IhrOrgSearchResponse response = ihrOrgService.searchDepartments(request);
            if (!response.isSuccess()) {
                log.info("【{}】查询部门终止, code={}, message={}", BIZ, response.getCode(), response.getMessage());
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        String.format("IHR 部门查询业务失败: code=%d, message=%s", response.getCode(), response.getMessage()));
            }
            log.info("【{}】查询部门成功, totalElements={}, returned={}, end={}",
                    BIZ, response.getTotalElements(),
                    response.getData() == null ? 0 : response.getData().size(),
                    response.getEnd());
            return response;

        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】查询部门终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】IHR HTTP 调用失败", BIZ, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "IHR 部门查询失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.info("【{}】查询部门终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】未知异常", BIZ, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "IHR 部门查询系统异常", e);
        }
    }
}
