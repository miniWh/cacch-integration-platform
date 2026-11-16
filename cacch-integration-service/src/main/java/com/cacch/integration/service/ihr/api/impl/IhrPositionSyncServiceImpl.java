package com.cacch.integration.service.ihr.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.IhrPositionSyncClient;
import com.cacch.integration.integration.ihr.client.dto.IhrPosition;
import com.cacch.integration.integration.ihr.client.dto.IhrPositionIdsResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrPositionListResponse;
import com.cacch.integration.service.ihr.api.IIhrPositionSyncService;
import com.cacch.integration.service.ihr.api.IIhrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * IHR 岗位主数据（MDM）同步链路拉取 Service 实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IhrPositionSyncServiceImpl implements IIhrPositionSyncService {

    private static final String BIZ = "IHR 岗位同步服务";

    /**
     * 按 ID 集合查询的单批上限（文档未标上限，保守取 100）
     */
    private static final int LIST_BY_IDS_BATCH_SIZE = 100;

    private final IIhrTokenService ihrTokenService;
    private final IhrPositionSyncClient ihrPositionSyncClient;

    @Override
    public List<IhrPosition> fetchAllPositions() {
        String accessToken = ihrTokenService.getAccessToken();
        IhrPositionListResponse resp;
        try {
            resp = ihrPositionSyncClient.listAll(accessToken);
        } catch (RestClientException e) {
            // 401/403 等鉴权失败时强制刷新 token，再重试一次
            log.info("【{}】全量拉取职位失败，尝试强制刷新 token 后重试, reason={}", BIZ, e.getMessage());
            String newToken = ihrTokenService.forceRefresh();
            try {
                resp = ihrPositionSyncClient.listAll(newToken);
            } catch (RestClientException retryError) {
                log.info("【{}】全量拉取职位重试仍失败, reason={}", BIZ, retryError.getMessage());
                log.error("【{}】全量拉取职位 HTTP 调用失败", BIZ, retryError);
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "IHR 全量拉取职位失败: " + retryError.getMessage(), retryError);
            }
        }

        if (resp == null || !resp.isSuccess()) {
            log.info("【{}】全量拉取职位终止, code={}, message={}, reason=业务失败",
                    BIZ, resp == null ? "null" : resp.getCode(), resp == null ? "空响应" : resp.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 全量拉取职位失败: code=" + (resp == null ? "null" : resp.getCode())
                            + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
        }

        List<IhrPosition> data = resp.getData();
        if (CollectionUtils.isEmpty(data)) {
            log.info("【{}】全量拉取职位返回空, reason=无数据", BIZ);
            return List.of();
        }
        log.info("【{}】fetchAllPositions 完成, 职位总数={}", BIZ, data.size());
        return data;
    }

    /**
     * 根据职位 ID 集合批量获取职位清单（100/批，汇总返回）
     */
    @Override
    public List<IhrPosition> listPositionsByIds(List<String> positionIds) {
        if (CollectionUtils.isEmpty(positionIds)) {
            log.info("【{}】按ID集合查询职位终止, reason=positionIds 为空", BIZ);
            return List.of();
        }
        List<IhrPosition> all = new ArrayList<>();
        for (int from = 0; from < positionIds.size(); from += LIST_BY_IDS_BATCH_SIZE) {
            List<String> batch = positionIds.subList(from,
                    Math.min(from + LIST_BY_IDS_BATCH_SIZE, positionIds.size()));
            all.addAll(listByIdsOneBatch(batch));
        }
        log.info("【{}】按ID集合查询职位完成, 请求ID数={}, 返回职位数={}", BIZ, positionIds.size(), all.size());
        return all;
    }

    /**
     * 单批次调用（401 强刷重试 + 业务码校验）
     */
    private List<IhrPosition> listByIdsOneBatch(List<String> batch) {
        String accessToken = ihrTokenService.getAccessToken();
        IhrPositionIdsResponse resp;
        try {
            resp = ihrPositionSyncClient.listByIds(accessToken, batch);
        } catch (RestClientException e) {
            log.info("【{}】按ID集合查询职位失败, 尝试强制刷新 token 后重试, positionIdCount={}, reason={}",
                    BIZ, batch.size(), e.getMessage());
            String newToken = ihrTokenService.forceRefresh();
            try {
                resp = ihrPositionSyncClient.listByIds(newToken, batch);
            } catch (RestClientException retryError) {
                log.info("【{}】按ID集合查询职位重试仍失败, positionIdCount={}, reason={}",
                        BIZ, batch.size(), retryError.getMessage());
                log.error("【{}】按ID集合查询职位 HTTP 调用失败, positionIdCount={}", BIZ, batch.size(), retryError);
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "IHR 根据职位id获取职位清单失败: " + retryError.getMessage(), retryError);
            }
        }

        if (resp == null || !resp.isSuccess()) {
            log.info("【{}】按ID集合查询职位接口终止, positionIdCount={}, code={}, message={}",
                    BIZ, batch.size(), resp == null ? "null" : resp.getCode(),
                    resp == null ? "空响应" : resp.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 根据职位id获取职位清单失败: code=" + (resp == null ? "null" : resp.getCode())
                            + ", message=" + (resp == null ? "空响应" : resp.getMessage()));
        }

        List<IhrPosition> data = resp.getData();
        if (CollectionUtils.isEmpty(data)) {
            log.info("【{}】按ID集合查询职位返回空, positionIdCount={}", BIZ, batch.size());
            return List.of();
        }
        return data;
    }
}
