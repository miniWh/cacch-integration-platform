package com.cacch.integration.service.ihr.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.IhrOrgClient;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentDetailResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentListResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
import com.cacch.integration.service.ihr.api.IIhrOrgService;
import com.cacch.integration.service.ihr.api.IIhrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 全量拉取部门清单每页条数
     */
    private static final int DEPT_PAGE_SIZE = 100;

    /**
     * 翻页安全上限（防止 end / totalPages 均缺失时死循环）
     */
    private static final int MAX_PAGES = 1000;

    /**
     * 按 ID 集合查询的单批上限（文档未标上限，保守取 100）
     */
    private static final int LIST_BY_IDS_BATCH_SIZE = 100;

    /**
     * list 接口「无上级部门」的 parentId 哨兵值
     */
    private static final String PARENT_ID_NO_PARENT = "-1";

    @Override
    public List<IhrDepartment> fetchAllDepartments() {
        List<IhrDepartment> all = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            IhrOrgSearchRequest request = new IhrOrgSearchRequest();
            request.setPage(page);
            request.setSize(DEPT_PAGE_SIZE);

            IhrOrgSearchResponse resp = searchDepartments(request);
            if (resp == null || !resp.isSuccess()) {
                log.info("【{}】全量拉取部门终止, page={}, code={}, message={}, reason=业务失败",
                        BIZ, page, resp == null ? "null" : resp.getCode(),
                        resp == null ? "空响应" : resp.getMessage());
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "IHR 全量拉取部门失败: page=" + page
                                + ", code=" + (resp == null ? "null" : resp.getCode())
                                + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
            }
            if (CollectionUtils.isEmpty(resp.getData())) {
                if (page == 0) {
                    log.info("【{}】全量拉取部门返回空, reason=首页即无数据", BIZ);
                    return List.of();
                }
                break;
            }
            all.addAll(resp.getData());

            Integer totalPages = resp.getTotalPages();
            if (Boolean.TRUE.equals(resp.getEnd()) || (totalPages != null && page >= totalPages - 1)) {
                break;
            }
        }
        log.info("【{}】fetchAllDepartments 完成, 部门总数={}", BIZ, all.size());
        return all;
    }

    /**
     * 根据部门 ID 集合批量获取部门详情（100/批，汇总返回）
     */
    @Override
    public List<IhrDepartment> listDepartmentsByIds(List<Long> departmentIds) {
        if (CollectionUtils.isEmpty(departmentIds)) {
            log.info("【{}】按ID集合查询部门终止, reason=departmentIds 为空", BIZ);
            return List.of();
        }
        List<IhrDepartment> all = new ArrayList<>();
        for (int from = 0; from < departmentIds.size(); from += LIST_BY_IDS_BATCH_SIZE) {
            List<Long> batch = departmentIds.subList(from, Math.min(from + LIST_BY_IDS_BATCH_SIZE, departmentIds.size()));
            all.addAll(listByIdsOneBatch(batch));
        }
        log.info("【{}】按ID集合查询部门完成, 请求ID数={}, 返回部门数={}", BIZ, departmentIds.size(), all.size());
        return all;
    }

    /**
     * 单批次调用（401 强刷重试 + 业务码校验 + parentId -1 归一化）
     */
    private List<IhrDepartment> listByIdsOneBatch(List<Long> batch) {
        String accessToken = ihrTokenService.getAccessToken();
        IhrDepartmentListResponse resp;
        try {
            resp = ihrOrgClient.listDepartmentsByIds(accessToken, batch);
        } catch (RestClientException e) {
            log.info("【{}】按ID集合查询部门失败, 尝试强制刷新 token 后重试, deptIdCount={}, reason={}",
                    BIZ, batch.size(), e.getMessage());
            String newToken = ihrTokenService.forceRefresh();
            try {
                resp = ihrOrgClient.listDepartmentsByIds(newToken, batch);
            } catch (RestClientException retryError) {
                log.info("【{}】按ID集合查询部门重试仍失败, deptIdCount={}, reason={}",
                        BIZ, batch.size(), retryError.getMessage());
                log.error("【{}】按ID集合查询部门 HTTP 调用失败, deptIdCount={}", BIZ, batch.size(), retryError);
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "IHR 根据部门id集合获取部门详情失败: " + retryError.getMessage(), retryError);
            }
        }

        if (resp == null || !resp.isSuccess()) {
            log.info("【{}】按ID集合查询部门接口终止, deptIdCount={}, code={}, message={}",
                    BIZ, batch.size(), resp == null ? "null" : resp.getCode(),
                    resp == null ? "空响应" : resp.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 根据部门id集合获取部门详情失败: code=" + (resp == null ? "null" : resp.getCode())
                            + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
        }

        List<IhrDepartment> data = resp.getData();
        if (CollectionUtils.isEmpty(data)) {
            log.info("【{}】按ID集合查询部门返回空, deptIdCount={}", BIZ, batch.size());
            return List.of();
        }
        // parentId 归一化：list 接口 -1 表示无上级，统一转 null（对齐搜索接口语义，保证 rowHash 跨接口稳定）
        for (IhrDepartment dept : data) {
            if (PARENT_ID_NO_PARENT.equals(dept.getParentId())) {
                dept.setParentId(null);
            }
        }
        return data;
    }

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
    public IhrDepartmentDetailResponse getDepartmentDetail(String departmentCode) {
        if (!StringUtils.hasText(departmentCode)) {
            log.info("【{}】获取部门详情终止, reason=departmentCode 为空", BIZ);
            throw new BizException(ResultCode.PARAM_ERROR, "departmentCode 不能为空");
        }

        String accessToken = ihrTokenService.getAccessToken();
        IhrDepartmentDetailResponse resp;
        try {
            resp = ihrOrgClient.getDepartmentDetail(accessToken, departmentCode);
        } catch (RestClientException e) {
            // 401/403 等鉴权失败时强制刷新 token，再重试一次
            log.info("【{}】获取部门详情失败，尝试强制刷新 token 后重试, departmentCode={}, reason={}", BIZ, departmentCode, e.getMessage());
            String newToken = ihrTokenService.forceRefresh();
            try {
                resp = ihrOrgClient.getDepartmentDetail(newToken, departmentCode);
            } catch (RestClientException retryError) {
                log.info("【{}】获取部门详情重试仍失败, departmentCode={}, reason={}", BIZ, departmentCode, retryError.getMessage());
                log.error("【{}】获取部门详情 HTTP 调用失败, departmentCode={}", BIZ, departmentCode, retryError);
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "IHR 获取部门详情失败: departmentCode=" + departmentCode + ", reason=" + retryError.getMessage(), retryError);
            }
        }

        if (resp == null || !resp.isSuccess()) {
            log.info("【{}】获取部门详情接口终止, departmentCode={}, code={}, message={}",
                    BIZ, departmentCode, resp == null ? "null" : resp.getCode(),
                    resp == null ? "空响应" : resp.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 获取部门详情失败: departmentCode=" + departmentCode
                            + ", code=" + (resp == null ? "null" : resp.getCode())
                            + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
        }
        if (resp.getData() == null) {
            log.info("【{}】获取部门详情返回空 data, departmentCode={}, reason=响应无 data", BIZ, departmentCode);
        }
        return resp;
    }
}
