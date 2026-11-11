package com.cacch.integration.service.ihr.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.IhrStaffClient;
import com.cacch.integration.integration.ihr.client.dto.IhrPageInfo;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffIdsData;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffIdsResponse;
import com.cacch.integration.service.ihr.api.IIhrStaffService;
import com.cacch.integration.service.ihr.api.IIhrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * IHR 员工域业务 Service 实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IhrStaffServiceImpl implements IIhrStaffService {

    private static final String BIZ = "IHR员工域Service";

    private final IIhrTokenService ihrTokenService;
    private final IhrStaffClient ihrStaffClient;

    @Override
    public List<String> fetchAllStaffIds(String staffStatus) {
        String accessToken = ihrTokenService.getAccessToken();

        // 第 1 页 — 先判断是否需要全量翻页
        IhrStaffIdsResponse first;
        try {
            first = ihrStaffClient.fetchStaffIdsPage(accessToken, staffStatus, 1);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.info("【{}】HTTP {}，强制刷新 token 后重试, staffStatus={}", BIZ, e.getStatusCode().value(), staffStatus);
                accessToken = ihrTokenService.forceRefresh();
                first = ihrStaffClient.fetchStaffIdsPage(accessToken, staffStatus, 1);
            } else {
                log.info("【{}】HTTP 调用终止, status={}, staffStatus={}", BIZ, e.getStatusCode().value(), staffStatus);
                throw e;
            }
        } catch (RestClientException e) {
            log.info("【{}】HTTP 调用终止, staffStatus={}, reason={}", BIZ, staffStatus, e.getMessage());
            throw e;
        }

        if (first == null || !first.isSuccess()) {
            log.info("【{}】接口终止, staffStatus={}, code={}, message={}",
                    BIZ, staffStatus, first == null ? "null" : first.getCode(),
                    first == null ? "空响应" : first.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 员工ID清单拉取失败: code=" + (first == null ? "null" : first.getCode())
                            + ", message=" + (first == null ? "空响应" : first.getMessage()));
        }

        IhrStaffIdsData data = first.getData();
        if (data == null || data.getDataList() == null || data.getDataList().isEmpty()) {
            log.info("【{}】fetchAllStaffIds 返回空, staffStatus={}, reason=无员工数据", BIZ, staffStatus);
            return Collections.emptyList();
        }

        List<String> all = new ArrayList<>(data.getDataList());
        IhrPageInfo pageInfo = data.getPageInfo();
        int totalPages = pageInfo == null ? 1 : pageInfo.getTotalPages();
        if (totalPages < 1) {
            totalPages = 1;
        }

        // 翻页拉取后续页
        if (totalPages > 1) {
            for (int p = 2; p <= totalPages; p++) {
                IhrStaffIdsResponse resp;
                try {
                    resp = ihrStaffClient.fetchStaffIdsPage(accessToken, staffStatus, p);
                } catch (HttpStatusCodeException e) {
                    if (e.getStatusCode() == HttpStatus.UNAUTHORIZED
                            || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                        log.info("【{}】翻页 HTTP {}, 强制刷新 token 后重试, pageNo={}, staffStatus={}",
                                BIZ, e.getStatusCode().value(), p, staffStatus);
                        accessToken = ihrTokenService.forceRefresh();
                        resp = ihrStaffClient.fetchStaffIdsPage(accessToken, staffStatus, p);
                    } else {
                        log.info("【{}】翻页终止, pageNo={}, staffStatus={}, reason=HTTP {}",
                                BIZ, p, staffStatus, e.getStatusCode().value());
                        throw e;
                    }
                } catch (RestClientException e) {
                    log.info("【{}】翻页终止, pageNo={}, staffStatus={}, reason={}",
                            BIZ, p, staffStatus, e.getMessage());
                    throw e;
                }

                if (resp == null || !resp.isSuccess()) {
                    log.info("【{}】翻页接口终止, pageNo={}, staffStatus={}, code={}, message={}",
                            BIZ, p, staffStatus, resp == null ? "null" : resp.getCode(),
                            resp == null ? "空响应" : resp.getMessage());
                    throw new BizException(ResultCode.INTEGRATION_ERROR,
                            "IHR 员工ID清单翻页拉取失败: pageNo=" + p
                                    + ", code=" + (resp == null ? "null" : resp.getCode())
                                    + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
                }

                IhrStaffIdsData pageData = resp.getData();
                if (pageData != null && pageData.getDataList() != null && !pageData.getDataList().isEmpty()) {
                    all.addAll(pageData.getDataList());
                }
            }
        }

        log.info("【{}】fetchAllStaffIds 完成, staffStatus={}, 总页数={}, 总条数={}", BIZ, staffStatus, totalPages, all.size());
        return all;
    }
}
