package com.cacch.integration.manager.fdd.api.impl;

import com.cacch.integration.common.config.fdd.FddProperties;
import com.cacch.integration.common.constant.fdd.FddConstants;
import com.cacch.integration.common.dto.fdd.FddAuthQueryResult;
import com.cacch.integration.common.dto.fdd.FddEnterpriseAuthQueryCommand;
import com.cacch.integration.common.enums.fdd.FddAuthStatusEnum;
import com.cacch.integration.common.enums.fdd.FddAuthTypeEnum;
import com.cacch.integration.common.enums.fdd.FddSourceSystemEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.fdd.FddEnterpriseAuthDO;
import com.cacch.integration.integration.fdd.client.FddClient;
import com.cacch.integration.integration.fdd.client.dto.FddCallbackRequest;
import com.cacch.integration.integration.fdd.client.dto.FddEnterpriseAuthRequest;
import com.cacch.integration.integration.fdd.client.dto.FddEnterpriseAuthResponse;
import com.cacch.integration.manager.fdd.api.IFddAuthManager;
import com.cacch.integration.service.fdd.api.IFddEnterpriseAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 法大大认证编排实现（企业认证）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FddAuthManagerImpl implements IFddAuthManager {

    private static final String LOG_BIZ = "FddAuth";

    private final IFddEnterpriseAuthService fddEnterpriseAuthService;
    private final FddClient fddClient;
    private final FddProperties fddProperties;

    @Override
    public FddAuthQueryResult queryOrAuthEnterprise(FddEnterpriseAuthQueryCommand command) {
        if (command == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "企业认证查询请求不能为空");
        }
        String internalCompanyName = trimRequired(command.internalCompanyName(), "internalCompanyName");
        String uscc = trimRequired(command.uscc(), "uscc");
        validateInternalCompany(internalCompanyName);

        FddEnterpriseAuthDO success = fddEnterpriseAuthService.findSuccess(internalCompanyName, uscc);
        if (success != null) {
            log.info("【{}】企业认证已通过, internalCompany={}, uscc={}", LOG_BIZ, internalCompanyName, uscc);
            return toEnterpriseResult(success, true, false, null, null);
        }

        FddEnterpriseAuthDO pending = fddEnterpriseAuthService.findLatestPending(internalCompanyName, uscc);
        if (pending != null) {
            log.info("【{}】企业认证处理中, internalCompany={}, uscc={}, id={}",
                    LOG_BIZ, internalCompanyName, uscc, pending.getId());
            return toEnterpriseResult(pending, false, true, null, "认证处理中");
        }

        FddEnterpriseAuthDO failed = fddEnterpriseAuthService.findLatestFailed(internalCompanyName, uscc);
        boolean autoAuth = command.autoAuth() == null || command.autoAuth();
        if (!autoAuth) {
            if (failed != null) {
                log.info("【{}】企业认证仅查询返回 FAILED, internalCompany={}, uscc={}",
                        LOG_BIZ, internalCompanyName, uscc);
                return toEnterpriseResult(failed, false, false, true, failed.getFailReason());
            }
            log.info("【{}】企业认证无记录且 autoAuth=false, internalCompany={}, uscc={}",
                    LOG_BIZ, internalCompanyName, uscc);
            return new FddAuthQueryResult(
                    false, false, null, FddAuthTypeEnum.ENTERPRISE.getCode(),
                    internalCompanyName, null, command.enterpriseName(), uscc,
                    null, null, null, null, null, null, true, "需要发起实名认证");
        }

        return initiateEnterpriseAuth(command, internalCompanyName, uscc, failed != null
                || fddEnterpriseAuthService.hasFailedHistory(internalCompanyName, uscc));
    }

    @Override
    public void handleEnterpriseCallback(FddCallbackRequest request, Object rawPayload) {
        if (request == null || !StringUtils.hasText(request.getTransactionNo())) {
            log.info("【{}】企业回调终止, reason=transactionNo 为空", LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "回调 transactionNo 不能为空");
        }
        String transactionNo = request.getTransactionNo().trim();
        FddEnterpriseAuthDO record = fddEnterpriseAuthService.findByTransactionNo(transactionNo);
        if (record == null) {
            log.info("【{}】企业回调终止, reason=未匹配到记录, transactionNo={}", LOG_BIZ, transactionNo);
            throw new BizException(ResultCode.PARAM_INVALID, "未找到对应企业认证记录, transactionNo=" + transactionNo);
        }
        if (FddAuthStatusEnum.SUCCESS.getCode().equals(record.getAuthStatus())
                || FddAuthStatusEnum.FAILED.getCode().equals(record.getAuthStatus())) {
            log.info("【{}】企业回调幂等跳过, id={}, status={}, transactionNo={}",
                    LOG_BIZ, record.getId(), record.getAuthStatus(), transactionNo);
            return;
        }

        Integer status = request.getStatus();
        if (status == null) {
            log.info("【{}】企业回调保持 PENDING, reason=status 为空, transactionNo={}", LOG_BIZ, transactionNo);
            return;
        }
        if (status == FddConstants.ENTERPRISE_STATUS_CERTIFIED) {
            fddEnterpriseAuthService.updateByCallback(
                    record.getId(), FddAuthStatusEnum.SUCCESS.getCode(), rawPayload, null);
            log.info("【{}】企业回调认证通过, id={}, uscc={}, transactionNo={}",
                    LOG_BIZ, record.getId(), record.getUscc(), transactionNo);
            return;
        }
        if (status == FddConstants.ENTERPRISE_STATUS_FAILED) {
            String failReason = "法大大企业认证失败, status=" + status;
            fddEnterpriseAuthService.updateByCallback(
                    record.getId(), FddAuthStatusEnum.FAILED.getCode(), rawPayload, failReason);
            log.info("【{}】企业回调认证失败, id={}, uscc={}, transactionNo={}",
                    LOG_BIZ, record.getId(), record.getUscc(), transactionNo);
            return;
        }
        log.info("【{}】企业回调中间状态保持 PENDING, id={}, status={}, transactionNo={}",
                LOG_BIZ, record.getId(), status, transactionNo);
    }

    private FddAuthQueryResult initiateEnterpriseAuth(FddEnterpriseAuthQueryCommand command,
                                                      String internalCompanyName,
                                                      String uscc,
                                                      boolean repeat) {
        String enterpriseName = trimRequired(command.enterpriseName(), "enterpriseName");
        FddSourceSystemEnum sourceSystem = FddSourceSystemEnum.fromCode(command.sourceSystem());
        if (sourceSystem == null) {
            log.info("【{}】发起企业认证终止, reason=sourceSystem 非法, value={}", LOG_BIZ, command.sourceSystem());
            throw new BizException(ResultCode.PARAM_INVALID, "sourceSystem 仅允许 CRM 或 OA");
        }
        if (!StringUtils.hasText(fddProperties.getCallbackUrl())) {
            log.info("【{}】发起企业认证终止, reason=callbackUrl 未配置", LOG_BIZ);
            throw new BizException(ResultCode.PARAM_INVALID, "法大大回调地址未配置");
        }

        FddEnterpriseAuthRequest request = FddEnterpriseAuthRequest.builder()
                .tpOrgId(uscc)
                .verifiedChannel(FddConstants.VERIFIED_CHANNEL_STANDARD)
                .verifiedWay(fddProperties.getEnterpriseVerifiedWay())
                .isRepeatVerified(repeat ? FddConstants.REPEAT_VERIFY : FddConstants.FIRST_VERIFY)
                .companyInfoDTO(FddEnterpriseAuthRequest.CompanyInfoDTO.builder()
                        .companyName(enterpriseName)
                        .creditCode(uscc)
                        .build())
                .applicationType(FddConstants.APPLICATION_TYPE_ALL)
                .notifyUrl(fddProperties.getCallbackUrl())
                .isSendSms(FddConstants.SEND_SMS_NO)
                .pageModify(FddConstants.PAGE_MODIFY_FORBIDDEN)
                .build();

        // HTTP 调用在事务外执行
        FddEnterpriseAuthResponse response = fddClient.getEnterpriseAuthUrl(request);
        String authUrl = response.getData().getUrl();
        String transactionNo = response.getData().getTransactionNo();

        Map<String, Object> requestDetail = new LinkedHashMap<>();
        requestDetail.put("request", request);
        requestDetail.put("response", response);

        FddEnterpriseAuthDO record = new FddEnterpriseAuthDO();
        record.setInternalCompanyName(internalCompanyName);
        record.setEnterpriseName(enterpriseName);
        record.setUscc(uscc);
        record.setAuthUrl(authUrl);
        record.setTransactionNo(transactionNo);
        record.setSourceSystem(sourceSystem.getCode());
        record.setSourceBizNo(StringUtils.hasText(command.sourceBizNo()) ? command.sourceBizNo().trim() : null);
        record.setRequestDetail(requestDetail);

        FddEnterpriseAuthDO saved = fddEnterpriseAuthService.insertPending(record);
        log.info("【{}】企业认证已发起, id={}, internalCompany={}, uscc={}, transactionNo={}",
                LOG_BIZ, saved.getId(), internalCompanyName, uscc, transactionNo);
        return toEnterpriseResult(saved, false, true, null, "已发起认证，请引导用户完成实名");
    }

    private void validateInternalCompany(String internalCompanyName) {
        if (!fddProperties.isAllowedInternalCompany(internalCompanyName)) {
            log.info("【{}】内部企业校验失败, internalCompany={}", LOG_BIZ, internalCompanyName);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "internalCompanyName 不在允许列表内: " + internalCompanyName);
        }
    }

    private static String trimRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ResultCode.PARAM_MISSING, fieldName + " 不能为空");
        }
        return value.trim();
    }

    private static FddAuthQueryResult toEnterpriseResult(FddEnterpriseAuthDO record,
                                                         boolean certified,
                                                         boolean needAuth,
                                                         Boolean canRetry,
                                                         String message) {
        return new FddAuthQueryResult(
                certified,
                needAuth,
                record.getAuthStatus(),
                FddAuthTypeEnum.ENTERPRISE.getCode(),
                record.getInternalCompanyName(),
                record.getAuthUrl(),
                record.getEnterpriseName(),
                record.getUscc(),
                null,
                null,
                null,
                record.getSourceSystem(),
                record.getFailReason(),
                record.getCertifiedAt(),
                canRetry,
                message
        );
    }
}
