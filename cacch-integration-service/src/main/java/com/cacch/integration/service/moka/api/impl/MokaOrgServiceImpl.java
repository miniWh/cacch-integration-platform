package com.cacch.integration.service.moka.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.moka.client.MokaOrgClient;
import com.cacch.integration.integration.moka.client.dto.MokaDeptListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncResponse;
import com.cacch.integration.service.moka.api.IMokaOrgService;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Moka 组织架构服务实现
 *
 * <p>Moka 鉴权为固定 Basic Auth（API Key 写入 Header），不像 IHR 那样需要 token 刷新重试，
 * 因此异常路径直接包装为 BizException 上抛。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaOrgServiceImpl implements IMokaOrgService {

    private static final String BIZ = "Moka 组织架构服务实现";

    private final MokaOrgClient mokaOrgClient;

    @Override
    public MokaDeptSyncResponse syncDepartmentsFull(MokaDeptSyncRequest request) {
        if (request == null) {
            log.info("【{}】全量同步终止, reason=请求体为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "Moka 组织架构同步请求体为空");
        }

        log.info("【{}】开始全量同步, departmentCount={}",
                BIZ, request.getDepartments() == null ? 0 : request.getDepartments().size());

        try {
            MokaDeptSyncResponse response = mokaOrgClient.syncDepartmentsFull(request);
            if (!response.isSuccess()) {
                log.info("【{}】全量同步终止, code={}, msg={}",
                        BIZ, response.getCode(), response.getMsg());
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        String.format("Moka 组织架构同步业务失败: code=%d, msg=%s",
                                response.getCode(), response.getMsg()));
            }
            log.info("【{}】全量同步成功, new={}, update={}, delete={}",
                    BIZ, response.getNewCount(), response.getUpdateCount(), response.getDeleteCount());
            return response;

        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】全量同步终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】Moka HTTP 调用失败", BIZ, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "Moka 组织架构同步失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.info("【{}】全量同步终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】未知异常", BIZ, e);
            throw new BizException(ResultCode.SYSTEM_ERROR,
                    "Moka 组织架构同步系统异常", e);
        }
    }

    @Override
    public MokaDeptListResponse getDepartments(String updateTimeStart) {
        log.info("【{}】开始获取全量组织架构, updateTimeStart={}",
                BIZ, StringUtils.hasText(updateTimeStart) ? updateTimeStart : "全量");

        try {
            MokaDeptListResponse response = mokaOrgClient.getDepartments(updateTimeStart);
            if (!response.isSuccess()) {
                log.info("【{}】获取全量组织架构终止, code={}, msg={}",
                        BIZ, response.getCode(), response.getMsg());
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        String.format("Moka 获取全量组织架构业务失败: code=%d, msg=%s",
                                response.getCode(), response.getMsg()));
            }
            int deptCount = response.getData() == null ? 0 : response.getData().size();
            log.info("【{}】获取全量组织架构成功, deptCount={}", BIZ, deptCount);
            return response;

        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】获取全量组织架构终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】Moka HTTP 调用失败", BIZ, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "Moka 获取全量组织架构失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.info("【{}】获取全量组织架构终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】未知异常", BIZ, e);
            throw new BizException(ResultCode.SYSTEM_ERROR,
                    "Moka 获取全量组织架构系统异常", e);
        }
    }
}
