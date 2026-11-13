package com.cacch.integration.service.ihr.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.IhrOrgClient;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentDetailResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
import com.cacch.integration.service.ihr.api.IIhrOrgService;
import com.cacch.integration.service.ihr.api.IIhrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

/**
 * IHR 组织架构/部门服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IhrOrgServiceImpl implements IIhrOrgService {

    private final IIhrTokenService ihrTokenService;
    private final IhrOrgClient ihrOrgClient;

    private static final String BIZ = "IHR 组织架构/部门服务实现";

    @Override
    public IhrOrgSearchResponse searchDepartments(IhrOrgSearchRequest request) {
        String accessToken = ihrTokenService.getAccessToken();
        try {
            return ihrOrgClient.searchDepartments(accessToken, request);
        } catch (RestClientException e) {
            // 401/403 等鉴权失败时强制刷新 token，再重试一次
            log.info("【{}】查询部门失败，尝试强制刷新 token 后重试, reason={}", BIZ, e.getMessage());
            String newToken = ihrTokenService.forceRefresh();
            try {
                return ihrOrgClient.searchDepartments(newToken, request);
            } catch (RestClientException retryError) {
                log.info("【{}】重试仍失败, reason={}", BIZ, retryError.getMessage());
                log.error("【{}】查询部门 HTTP 调用失败", BIZ, retryError);
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "IHR 查询部门失败: " + retryError.getMessage(), retryError);
            }
        }
    }

    @Override
    public IhrDepartmentDetailResponse getDepartmentDetail(String deptId) {
        if (!StringUtils.hasText(deptId)) {
            log.info("【{}】获取部门详情终止, reason=deptId 为空", BIZ);
            throw new BizException(ResultCode.PARAM_ERROR, "deptId 不能为空");
        }

        String accessToken = ihrTokenService.getAccessToken();
        IhrDepartmentDetailResponse resp;
        try {
            resp = ihrOrgClient.getDepartmentDetail(accessToken, deptId);
        } catch (RestClientException e) {
            // 401/403 等鉴权失败时强制刷新 token，再重试一次
            log.info("【{}】获取部门详情失败，尝试强制刷新 token 后重试, deptId={}, reason={}", BIZ, deptId, e.getMessage());
            String newToken = ihrTokenService.forceRefresh();
            try {
                resp = ihrOrgClient.getDepartmentDetail(newToken, deptId);
            } catch (RestClientException retryError) {
                log.info("【{}】获取部门详情重试仍失败, deptId={}, reason={}", BIZ, deptId, retryError.getMessage());
                log.error("【{}】获取部门详情 HTTP 调用失败, deptId={}", BIZ, deptId, retryError);
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "IHR 获取部门详情失败: deptId=" + deptId + ", reason=" + retryError.getMessage(), retryError);
            }
        }

        if (resp == null || !resp.isSuccess()) {
            log.info("【{}】获取部门详情接口终止, deptId={}, code={}, message={}",
                    BIZ, deptId, resp == null ? "null" : resp.getCode(),
                    resp == null ? "空响应" : resp.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 获取部门详情失败: deptId=" + deptId
                            + ", code=" + (resp == null ? "null" : resp.getCode())
                            + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
        }
        if (resp.getData() == null) {
            log.info("【{}】获取部门详情返回空 data, deptId={}, reason=响应无 data", BIZ, deptId);
        }
        return resp;
    }
}
