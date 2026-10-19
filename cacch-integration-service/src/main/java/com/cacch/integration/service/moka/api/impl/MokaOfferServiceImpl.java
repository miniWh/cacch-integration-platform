package com.cacch.integration.service.moka.api.impl;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.moka.client.MokaOfferClient;
import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoRequest;
import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoResponse;
import com.cacch.integration.service.moka.api.IMokaOfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Moka offer 扩展信息服务实现
 *
 * <p>封装 Moka ATS offer 域的业务逻辑，异常路径统一包装为 BizException 上抛。
 * 租户 ID（orgId）由本服务从 {@link MokaProperties} 注入到上游请求体。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaOfferServiceImpl implements IMokaOfferService {

    private static final String BIZ = "Moka offer 扩展信息服务";

    /**
     * 单批次最大申请 ID 数量（参考 Moka syncInfo 批量限制经验）
     */
    private static final int MAX_BATCH_SIZE = 100;

    private final MokaOfferClient mokaOfferClient;
    private final MokaProperties mokaProperties;

    @Override
    public MokaOfferExtendInfoResponse getOfferExtendInfo(List<Long> applicationIds, Boolean offerAttachment) {
        // —— 参数校验：applicationIds 非空 ——
        if (CollectionUtils.isEmpty(applicationIds)) {
            log.info("【{}】获取offer扩展信息终止, reason=applicationIds 为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "applicationIds 不可为空");
        }
        // —— 参数校验：单批次不超过 100 ——
        if (applicationIds.size() > MAX_BATCH_SIZE) {
            log.info("【{}】获取offer扩展信息终止, reason=超过单批次上限, size={}, max={}",
                    BIZ, applicationIds.size(), MAX_BATCH_SIZE);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "applicationIds 单批次最多 " + MAX_BATCH_SIZE + " 条，当前 " + applicationIds.size() + " 条");
        }
        // —— 配置校验：API Key 已配置 ——
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】获取offer扩展信息终止, reason=Moka API Key 未配置", BIZ);
            throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED, "Moka API Key 未配置");
        }
        // —— 配置校验：orgId 已配置 ——
        if (!mokaProperties.isOrgIdConfigured()) {
            log.info("【{}】获取offer扩展信息终止, reason=Moka orgId 未配置", BIZ);
            throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED, "Moka orgId 未配置");
        }

        // —— 组装上游请求体：orgId 由配置注入 ——
        MokaOfferExtendInfoRequest request = new MokaOfferExtendInfoRequest();
        request.setOrgId(mokaProperties.getOrgId());
        request.setApplicationIds(applicationIds);
        request.setOfferAttachment(offerAttachment);

        log.info("【{}】开始获取offer扩展信息, orgId={}, size={}, offerAttachment={}",
                BIZ, mokaProperties.getOrgId(), applicationIds.size(), offerAttachment);

        try {
            MokaOfferExtendInfoResponse response = mokaOfferClient.getOfferExtendInfo(request);
            if (!response.isSuccess()) {
                log.info("【{}】获取offer扩展信息终止, code={}, msg={}",
                        BIZ, response.getCode(), response.getMsg());
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        String.format("Moka 获取offer扩展信息业务失败: code=%d, msg=%s",
                                response.getCode(), response.getMsg()));
            }
            log.info("【{}】获取offer扩展信息成功, size={}",
                    BIZ,
                    response.getData() != null ? response.getData().size() : 0);
            return response;

        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】获取offer扩展信息终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】Moka HTTP 调用失败", BIZ, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "Moka 获取offer扩展信息失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.info("【{}】获取offer扩展信息终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】未知异常", BIZ, e);
            throw new BizException(ResultCode.SYSTEM_ERROR,
                    "Moka 获取offer扩展信息系统异常", e);
        }
    }
}
