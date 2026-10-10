package com.cacch.integration.service.moka.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.moka.client.MokaApplicationClient;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignResponse;
import com.cacch.integration.service.moka.api.IMokaApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

/**
 * Moka 入离职服务实现
 *
 * <p>封装 Moka ATS 申请域（Application）的业务逻辑，异常路径统一包装为 BizException 上抛。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaApplicationServiceImpl implements IMokaApplicationService {

    private static final String BIZ = "Moka 入离职服务";

    private final MokaApplicationClient mokaApplicationClient;

    @Override
    public MokaApplicationResignResponse resignApplication(Integer applicationId, MokaApplicationResignRequest request) {
        // —— 参数校验：申请 id 非空且大于 0 ——
        if (applicationId == null || applicationId <= 0) {
            log.info("【{}】标记候选人已离职终止, reason=applicationId 非法, applicationId={}", BIZ, applicationId);
            throw new BizException(ResultCode.PARAM_INVALID, "applicationId 非法: " + applicationId);
        }
        // —— 参数校验：请求体与 detail 必填 ——
        if (request == null || !StringUtils.hasText(request.getDetail())) {
            log.info("【{}】标记候选人已离职终止, reason=请求体或 detail 为空, applicationId={}", BIZ, applicationId);
            throw new BizException(ResultCode.PARAM_MISSING, "离职原因 detail 必填");
        }

        log.info("【{}】开始标记候选人已离职, applicationId={}, isResignWriteJob={}",
                BIZ, applicationId, request.getIsResignWriteJob());

        try {
            MokaApplicationResignResponse response = mokaApplicationClient.resignApplication(applicationId, request);
            if (!response.isSuccess()) {
                log.info("【{}】标记候选人已离职终止, applicationId={}, code={}, errorMessage={}",
                        BIZ, applicationId, response.getCode(), response.getErrorMessage());
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        String.format("Moka 标记候选人已离职业务失败: code=%s, errorMessage=%s",
                                response.getCode(), response.getErrorMessage()));
            }
            log.info("【{}】标记候选人已离职成功, applicationId={}", BIZ, applicationId);
            return response;

        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】标记候选人已离职终止, reason={}, applicationId={}", BIZ, e.getMessage(), applicationId);
            log.error("【{}】Moka HTTP 调用失败, applicationId={}", BIZ, applicationId, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "Moka 标记候选人已离职失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.info("【{}】标记候选人已离职终止, reason={}, applicationId={}", BIZ, e.getMessage(), applicationId);
            log.error("【{}】未知异常, applicationId={}", BIZ, applicationId, e);
            throw new BizException(ResultCode.SYSTEM_ERROR,
                    "Moka 标记候选人已离职系统异常", e);
        }
    }
}
