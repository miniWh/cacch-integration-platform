package com.cacch.integration.service.ihr.api.impl;

import com.cacch.integration.common.constant.ihr.IhrStaffLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.IhrStaffClient;
import com.cacch.integration.integration.ihr.client.dto.IhrPageInfo;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicInfo;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffIdsData;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffIdsResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffModificationInfo;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffModificationsData;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffModificationsResponse;
import com.cacch.integration.service.ihr.api.IIhrStaffService;
import com.cacch.integration.service.ihr.api.IIhrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
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

    private static final String BIZ = IhrStaffLogConstants.LOG_BIZ;

    private final IIhrTokenService ihrTokenService;
    private final IhrStaffClient ihrStaffClient;

    @Override
    public List<String> fetchAllStaffIds(String staffStatus) {
        // 单元素数组作可变 token 引用：401 刷新后的新 token 写回 holder，跨页自动生效
        String[] tokenHolder = {ihrTokenService.getAccessToken()};

        // 第 1 页 — 同时探明总页数
        IhrStaffIdsData first = fetchStaffIdsPageData(tokenHolder, staffStatus, 1);
        if (first == null || CollectionUtils.isEmpty(first.getDataList())) {
            log.info("【{}】fetchAllStaffIds 返回空, staffStatus={}, reason=无员工数据", BIZ, staffStatus);
            return Collections.emptyList();
        }

        List<String> all = new ArrayList<>(first.getDataList());
        int totalPages = resolveTotalPages(first.getPageInfo());

        // 翻页拉取后续页（token 由 holder 自动携带最新值）
        for (int p = 2; p <= totalPages; p++) {
            IhrStaffIdsData page = fetchStaffIdsPageData(tokenHolder, staffStatus, p);
            if (page != null && !CollectionUtils.isEmpty(page.getDataList())) {
                all.addAll(page.getDataList());
            }
        }

        log.info("【{}】fetchAllStaffIds 完成, staffStatus={}, 总页数={}, 总条数={}", BIZ, staffStatus, totalPages, all.size());
        return all;
    }

    @Override
    public List<String> fetchAddedStaffIds(LocalDate date) {
        String[] tokenHolder = {ihrTokenService.getAccessToken()};

        IhrStaffIdsData first = fetchAdditionsPageData(tokenHolder, date, 1);
        if (first == null || CollectionUtils.isEmpty(first.getDataList())) {
            log.info("【{}】fetchAddedStaffIds 返回空, date={}, reason=无新增员工数据", BIZ, date);
            return Collections.emptyList();
        }

        List<String> all = new ArrayList<>(first.getDataList());
        int totalPages = resolveTotalPages(first.getPageInfo());

        for (int p = 2; p <= totalPages; p++) {
            IhrStaffIdsData page = fetchAdditionsPageData(tokenHolder, date, p);
            if (page != null && !CollectionUtils.isEmpty(page.getDataList())) {
                all.addAll(page.getDataList());
            }
        }

        log.info("【{}】fetchAddedStaffIds 完成, date={}, 总页数={}, 总条数={}", BIZ, date, totalPages, all.size());
        return all;
    }

    @Override
    public List<IhrStaffModificationInfo> fetchModifications(LocalDate date, String staffStatus) {
        String[] tokenHolder = {ihrTokenService.getAccessToken()};

        IhrStaffModificationsData first = fetchModificationsPageData(tokenHolder, date, staffStatus, 1);
        if (first == null || CollectionUtils.isEmpty(first.getDataList())) {
            log.info("【{}】fetchModifications 返回空, date={}, staffStatus={}, reason=无更新员工数据", BIZ, date, staffStatus);
            return Collections.emptyList();
        }

        List<IhrStaffModificationInfo> all = new ArrayList<>(first.getDataList());
        int totalPages = resolveTotalPages(first.getPageInfo());

        for (int p = 2; p <= totalPages; p++) {
            IhrStaffModificationsData page = fetchModificationsPageData(tokenHolder, date, staffStatus, p);
            if (page != null && !CollectionUtils.isEmpty(page.getDataList())) {
                all.addAll(page.getDataList());
            }
        }

        log.info("【{}】fetchModifications 完成, date={}, staffStatus={}, 总页数={}, 总条数={}",
                BIZ, date, staffStatus, totalPages, all.size());
        return all;
    }

    @Override
    public IhrStaffBasicInfo fetchStaffBasic(String staffId) {
        IhrStaffBasicResponse resp = fetchStaffBasicResponse(staffId);
        IhrStaffBasicInfo data = resp.getData();
        if (data == null) {
            log.info("【{}】fetchStaffBasic 返回空, staffId={}, reason=响应无 data", BIZ, staffId);
        }
        return data;
    }

    @Override
    public IhrStaffBasicResponse fetchStaffBasicResponse(String staffId) {
        if (!StringUtils.hasText(staffId)) {
            log.info("【{}】fetchStaffBasicResponse 终止, reason=staffId 为空", BIZ);
            throw new BizException(ResultCode.PARAM_ERROR, "staffId 不能为空");
        }

        String accessToken = ihrTokenService.getAccessToken();
        IhrStaffBasicResponse resp;
        try {
            resp = ihrStaffClient.fetchStaffBasic(accessToken, staffId);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.info("【{}】HTTP {}, 强制刷新 token 后重试, staffId={}", BIZ, e.getStatusCode().value(), staffId);
                accessToken = ihrTokenService.forceRefresh();
                resp = ihrStaffClient.fetchStaffBasic(accessToken, staffId);
            } else {
                log.info("【{}】HTTP 调用终止, status={}, staffId={}", BIZ, e.getStatusCode().value(), staffId);
                throw e;
            }
        } catch (RestClientException e) {
            log.info("【{}】HTTP 调用终止, staffId={}, reason={}", BIZ, staffId, e.getMessage());
            throw e;
        }

        if (resp == null || !resp.isSuccess()) {
            log.info("【{}】接口终止, staffId={}, code={}, message={}",
                    BIZ, staffId, resp == null ? "null" : resp.getCode(),
                    resp == null ? "空响应" : resp.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 员工基本信息拉取失败: staffId=" + staffId
                            + ", code=" + (resp == null ? "null" : resp.getCode())
                            + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
        }

        if (resp.getData() == null) {
            log.info("【{}】fetchStaffBasicResponse 返回空 data, staffId={}, reason=响应无 data", BIZ, staffId);
        }
        return resp;
    }

    // ==================== 单页拉取 + 401 刷新重试（3 类响应独立实现） ====================

    /**
     * 拉取全量员工 ID 清单单页 — 含 401/403 刷新 token 重试与业务失败判定
     */
    private IhrStaffIdsData fetchStaffIdsPageData(String[] tokenHolder, String staffStatus, int pageNo) {
        IhrStaffIdsResponse resp;
        try {
            resp = ihrStaffClient.fetchStaffIdsPage(tokenHolder[0], staffStatus, pageNo);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.info("【{}】HTTP {}, 强制刷新 token 后重试, pageNo={}, staffStatus={}", BIZ, e.getStatusCode().value(), pageNo, staffStatus);
                tokenHolder[0] = ihrTokenService.forceRefresh();
                resp = ihrStaffClient.fetchStaffIdsPage(tokenHolder[0], staffStatus, pageNo);
            } else {
                log.info("【{}】HTTP 调用终止, status={}, pageNo={}, staffStatus={}", BIZ, e.getStatusCode().value(), pageNo, staffStatus);
                throw e;
            }
        } catch (RestClientException e) {
            log.info("【{}】HTTP 调用终止, pageNo={}, staffStatus={}, reason={}", BIZ, pageNo, staffStatus, e.getMessage());
            throw e;
        }

        if (resp == null || !resp.isSuccess()) {
            log.info("【{}】接口终止, pageNo={}, staffStatus={}, code={}, message={}",
                    BIZ, pageNo, staffStatus, resp == null ? "null" : resp.getCode(),
                    resp == null ? "空响应" : resp.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 员工ID清单拉取失败: pageNo=" + pageNo
                            + ", code=" + (resp == null ? "null" : resp.getCode())
                            + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
        }
        return resp.getData();
    }

    /**
     * 拉取新增员工 ID 清单单页 — 含 401/403 刷新 token 重试与业务失败判定
     */
    private IhrStaffIdsData fetchAdditionsPageData(String[] tokenHolder, LocalDate date, int pageNo) {
        IhrStaffIdsResponse resp;
        try {
            resp = ihrStaffClient.fetchAddedStaffIdsPage(tokenHolder[0], date, pageNo);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.info("【{}】HTTP {}, 强制刷新 token 后重试, pageNo={}, date={}", BIZ, e.getStatusCode().value(), pageNo, date);
                tokenHolder[0] = ihrTokenService.forceRefresh();
                resp = ihrStaffClient.fetchAddedStaffIdsPage(tokenHolder[0], date, pageNo);
            } else {
                log.info("【{}】HTTP 调用终止, status={}, pageNo={}, date={}", BIZ, e.getStatusCode().value(), pageNo, date);
                throw e;
            }
        } catch (RestClientException e) {
            log.info("【{}】HTTP 调用终止, pageNo={}, date={}, reason={}", BIZ, pageNo, date, e.getMessage());
            throw e;
        }

        if (resp == null || !resp.isSuccess()) {
            log.info("【{}】接口终止, pageNo={}, date={}, code={}, message={}",
                    BIZ, pageNo, date, resp == null ? "null" : resp.getCode(),
                    resp == null ? "空响应" : resp.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 新增员工ID清单拉取失败: pageNo=" + pageNo + ", date=" + date
                            + ", code=" + (resp == null ? "null" : resp.getCode())
                            + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
        }
        return resp.getData();
    }

    /**
     * 拉取更新员工 ID 清单 V2 单页 — 含 401/403 刷新 token 重试与业务失败判定
     */
    private IhrStaffModificationsData fetchModificationsPageData(String[] tokenHolder, LocalDate date,
                                                                 String staffStatus, int pageNo) {
        IhrStaffModificationsResponse resp;
        try {
            resp = ihrStaffClient.fetchModificationsPage(tokenHolder[0], date, staffStatus, pageNo);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.info("【{}】HTTP {}, 强制刷新 token 后重试, pageNo={}, date={}, staffStatus={}",
                        BIZ, e.getStatusCode().value(), pageNo, date, staffStatus);
                tokenHolder[0] = ihrTokenService.forceRefresh();
                resp = ihrStaffClient.fetchModificationsPage(tokenHolder[0], date, staffStatus, pageNo);
            } else {
                log.info("【{}】HTTP 调用终止, status={}, pageNo={}, date={}, staffStatus={}",
                        BIZ, e.getStatusCode().value(), pageNo, date, staffStatus);
                throw e;
            }
        } catch (RestClientException e) {
            log.info("【{}】HTTP 调用终止, pageNo={}, date={}, staffStatus={}, reason={}",
                    BIZ, pageNo, date, staffStatus, e.getMessage());
            throw e;
        }

        if (resp == null || !resp.isSuccess()) {
            log.info("【{}】接口终止, pageNo={}, date={}, staffStatus={}, code={}, message={}",
                    BIZ, pageNo, date, staffStatus,
                    resp == null ? "null" : resp.getCode(),
                    resp == null ? "空响应" : resp.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 更新员工ID清单V2拉取失败: pageNo=" + pageNo + ", date=" + date
                            + ", staffStatus=" + staffStatus
                            + ", code=" + (resp == null ? "null" : resp.getCode())
                            + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
        }
        return resp.getData();
    }

    // ==================== 通用工具方法 ====================

    /**
     * 规范化总页数 — null 或小于 1 时兜底返回 1
     *
     * @param pageInfo 分页信息，可为 null
     * @return 不小于 1 的总页数
     */
    private int resolveTotalPages(IhrPageInfo pageInfo) {
        if (pageInfo == null || pageInfo.getTotalPages() == null || pageInfo.getTotalPages() < 1) {
            return 1;
        }
        return pageInfo.getTotalPages();
    }
}
