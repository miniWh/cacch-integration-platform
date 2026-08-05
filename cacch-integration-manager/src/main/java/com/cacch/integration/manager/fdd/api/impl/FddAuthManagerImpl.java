package com.cacch.integration.manager.fdd.api.impl;

import com.cacch.integration.common.config.fdd.FddProperties;
import com.cacch.integration.common.constant.fdd.FddConstants;
import com.cacch.integration.common.dto.fdd.FddAuthQueryResult;
import com.cacch.integration.common.dto.fdd.FddEnterpriseAuthQueryCommand;
import com.cacch.integration.common.dto.fdd.FddPersonAuthQueryCommand;
import com.cacch.integration.common.enums.fdd.FddAuthStatusEnum;
import com.cacch.integration.common.enums.fdd.FddAuthTypeEnum;
import com.cacch.integration.common.enums.fdd.FddSourceSystemEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.fdd.FddEnterpriseAuthDO;
import com.cacch.integration.entity.fdd.FddPersonAuthDO;
import com.cacch.integration.integration.fdd.client.FddClient;
import com.cacch.integration.integration.fdd.client.dto.FddCallbackRequest;
import com.cacch.integration.integration.fdd.client.dto.FddCreateAccountRequest;
import com.cacch.integration.integration.fdd.client.dto.FddCreateAccountResponse;
import com.cacch.integration.integration.fdd.client.dto.FddCreateCompanyRequest;
import com.cacch.integration.integration.fdd.client.dto.FddCreateCompanyResponse;
import com.cacch.integration.integration.fdd.client.dto.FddEnterpriseAuthRequest;
import com.cacch.integration.integration.fdd.client.dto.FddEnterpriseAuthResponse;
import com.cacch.integration.integration.fdd.client.dto.FddGetAccountResponse;
import com.cacch.integration.integration.fdd.client.dto.FddGetCompanyResponse;
import com.cacch.integration.integration.fdd.client.dto.FddPersonAuthRequest;
import com.cacch.integration.integration.fdd.client.dto.FddPersonAuthResponse;
import com.cacch.integration.manager.fdd.api.IFddAuthManager;
import com.cacch.integration.service.fdd.api.IFddEnterpriseAuthService;
import com.cacch.integration.service.fdd.api.IFddPersonAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 法大大认证编排实现（企业 / 个人）
 *
 * <p>对齐法大大标准流程：个人 = 查询 → 创建用户 → 实名认证；企业 = 查询 →（管理员须已个人实名）创建企业并绑定管理员 → 实名认证。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FddAuthManagerImpl implements IFddAuthManager {

    private static final String LOG_BIZ = "FddAuth";
    private static final String AREA_CODE_CN = "+86";

    private final IFddEnterpriseAuthService fddEnterpriseAuthService;
    private final IFddPersonAuthService fddPersonAuthService;
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

        FddEnterpriseAuthDO synced = syncEnterpriseIfRemoteCertified(internalCompanyName, uscc, command.enterpriseName());
        if (synced != null) {
            log.info("【{}】企业认证法大大侧已通过并同步本地, internalCompany={}, uscc={}, fddCompanyId={}",
                    LOG_BIZ, internalCompanyName, uscc, synced.getFddCompanyId());
            return toEnterpriseResult(synced, true, false, null, null);
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
    public FddAuthQueryResult queryOrAuthPerson(FddPersonAuthQueryCommand command) {
        if (command == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "个人认证查询请求不能为空");
        }
        String internalCompanyName = trimRequired(command.internalCompanyName(), "internalCompanyName");
        String idNumber = trimRequired(command.idNumber(), "idNumber");
        validateInternalCompany(internalCompanyName);

        FddPersonAuthDO success = fddPersonAuthService.findSuccess(internalCompanyName, idNumber);
        if (success != null) {
            log.info("【{}】个人认证已通过, internalCompany={}, idNumber={}",
                    LOG_BIZ, internalCompanyName, maskIdNumber(idNumber));
            return toPersonResult(success, true, false, null, null);
        }

        FddPersonAuthDO synced = syncPersonIfRemoteCertified(
                internalCompanyName, idNumber, command.personName(), command.mobile());
        if (synced != null) {
            log.info("【{}】个人认证法大大侧已通过并同步本地, internalCompany={}, idNumber={}, fddAccountId={}",
                    LOG_BIZ, internalCompanyName, maskIdNumber(idNumber), synced.getFddAccountId());
            return toPersonResult(synced, true, false, null, null);
        }

        FddPersonAuthDO pending = fddPersonAuthService.findLatestPending(internalCompanyName, idNumber);
        if (pending != null) {
            log.info("【{}】个人认证处理中, internalCompany={}, idNumber={}, id={}",
                    LOG_BIZ, internalCompanyName, maskIdNumber(idNumber), pending.getId());
            return toPersonResult(pending, false, true, null, "认证处理中");
        }

        FddPersonAuthDO failed = fddPersonAuthService.findLatestFailed(internalCompanyName, idNumber);
        boolean autoAuth = command.autoAuth() == null || command.autoAuth();
        if (!autoAuth) {
            if (failed != null) {
                log.info("【{}】个人认证仅查询返回 FAILED, internalCompany={}, idNumber={}",
                        LOG_BIZ, internalCompanyName, maskIdNumber(idNumber));
                return toPersonResult(failed, false, false, true, failed.getFailReason());
            }
            log.info("【{}】个人认证无记录且 autoAuth=false, internalCompany={}, idNumber={}",
                    LOG_BIZ, internalCompanyName, maskIdNumber(idNumber));
            return new FddAuthQueryResult(
                    false, false, null, FddAuthTypeEnum.PERSON.getCode(),
                    internalCompanyName, null, null, null,
                    command.personName(), idNumber, command.mobile(),
                    null, null, null, true, "需要发起实名认证");
        }

        return initiatePersonAuth(command, internalCompanyName, idNumber, failed != null
                || fddPersonAuthService.hasFailedHistory(internalCompanyName, idNumber));
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
                    record.getId(), FddAuthStatusEnum.SUCCESS.getCode(), rawPayload, null,
                    request.getCompanyId(), request.getAccountId());
            log.info("【{}】企业回调认证通过, id={}, uscc={}, transactionNo={}",
                    LOG_BIZ, record.getId(), record.getUscc(), transactionNo);
            return;
        }
        if (status == FddConstants.ENTERPRISE_STATUS_FAILED) {
            String failReason = "法大大企业认证失败, status=" + status;
            fddEnterpriseAuthService.updateByCallback(
                    record.getId(), FddAuthStatusEnum.FAILED.getCode(), rawPayload, failReason,
                    request.getCompanyId(), request.getAccountId());
            log.info("【{}】企业回调认证失败, id={}, uscc={}, transactionNo={}",
                    LOG_BIZ, record.getId(), record.getUscc(), transactionNo);
            return;
        }
        log.info("【{}】企业回调中间状态保持 PENDING, id={}, status={}, transactionNo={}",
                LOG_BIZ, record.getId(), status, transactionNo);
    }

    @Override
    public void handlePersonCallback(FddCallbackRequest request, Object rawPayload) {
        if (request == null || !StringUtils.hasText(request.getTransactionNo())) {
            log.info("【{}】个人回调终止, reason=transactionNo 为空", LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "回调 transactionNo 不能为空");
        }
        String transactionNo = request.getTransactionNo().trim();
        FddPersonAuthDO record = fddPersonAuthService.findByTransactionNo(transactionNo);
        if (record == null) {
            log.info("【{}】个人回调终止, reason=未匹配到记录, transactionNo={}", LOG_BIZ, transactionNo);
            throw new BizException(ResultCode.PARAM_INVALID, "未找到对应个人认证记录, transactionNo=" + transactionNo);
        }
        if (FddAuthStatusEnum.SUCCESS.getCode().equals(record.getAuthStatus())
                || FddAuthStatusEnum.FAILED.getCode().equals(record.getAuthStatus())) {
            log.info("【{}】个人回调幂等跳过, id={}, status={}, transactionNo={}",
                    LOG_BIZ, record.getId(), record.getAuthStatus(), transactionNo);
            return;
        }

        Integer status = request.getStatus();
        if (status == null) {
            log.info("【{}】个人回调保持 PENDING, reason=status 为空, transactionNo={}", LOG_BIZ, transactionNo);
            return;
        }
        if (status == FddConstants.PERSON_STATUS_CERTIFIED) {
            fddPersonAuthService.updateByCallback(
                    record.getId(), FddAuthStatusEnum.SUCCESS.getCode(), rawPayload, null, request.getAccountId());
            log.info("【{}】个人回调认证通过, id={}, idNumber={}, transactionNo={}",
                    LOG_BIZ, record.getId(), maskIdNumber(record.getIdNumber()), transactionNo);
            return;
        }
        if (status == FddConstants.PERSON_STATUS_FAILED) {
            String failReason = "法大大个人认证失败, status=" + status;
            fddPersonAuthService.updateByCallback(
                    record.getId(), FddAuthStatusEnum.FAILED.getCode(), rawPayload, failReason, request.getAccountId());
            log.info("【{}】个人回调认证失败, id={}, idNumber={}, transactionNo={}",
                    LOG_BIZ, record.getId(), maskIdNumber(record.getIdNumber()), transactionNo);
            return;
        }
        log.info("【{}】个人回调中间状态保持 PENDING, id={}, status={}, transactionNo={}",
                LOG_BIZ, record.getId(), status, transactionNo);
    }

    private FddAuthQueryResult initiateEnterpriseAuth(FddEnterpriseAuthQueryCommand command,
                                                      String internalCompanyName,
                                                      String uscc,
                                                      boolean repeat) {
        String enterpriseName = trimRequired(command.enterpriseName(), "enterpriseName");
        String adminName = trimRequired(command.personName(), "personName（企业管理员）");
        String adminIdNumber = trimRequired(command.idNumber(), "idNumber（企业管理员）");
        String adminMobile = trimRequired(command.mobile(), "mobile（企业管理员）");
        FddSourceSystemEnum sourceSystem = requireSourceSystem(command.sourceSystem());
        requireCallbackUrl();

        FddPersonAuthDO adminPerson = fddPersonAuthService.findSuccess(internalCompanyName, adminIdNumber);
        if (adminPerson == null) {
            log.info("【{}】企业认证终止, reason=管理员未完成个人实名, internalCompany={}, idNumber={}",
                    LOG_BIZ, internalCompanyName, maskIdNumber(adminIdNumber));
            throw new BizException(ResultCode.PARAM_INVALID,
                    "发起企业认证前须先完成企业管理员个人实名认证（internalCompanyName + idNumber）");
        }

        CompanyBinding binding = ensureCompany(enterpriseName, uscc, adminName, adminIdNumber, adminMobile,
                adminPerson.getFddAccountId());

        FddEnterpriseAuthRequest request = FddEnterpriseAuthRequest.builder()
                .companyId(binding.companyId())
                .accountId(binding.accountId())
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

        FddEnterpriseAuthResponse response = fddClient.getEnterpriseAuthUrl(request);
        String authUrl = response.getData().getUrl();
        String transactionNo = response.getData().getTransactionNo();

        Map<String, Object> requestDetail = new LinkedHashMap<>();
        requestDetail.put("createOrGetCompany", Map.of(
                "companyId", binding.companyId(),
                "accountId", binding.accountId()));
        requestDetail.put("request", request);
        requestDetail.put("response", response);

        FddEnterpriseAuthDO record = new FddEnterpriseAuthDO();
        record.setInternalCompanyName(internalCompanyName);
        record.setEnterpriseName(enterpriseName);
        record.setUscc(uscc);
        record.setFddCompanyId(binding.companyId());
        record.setFddAccountId(binding.accountId());
        record.setAuthUrl(authUrl);
        record.setTransactionNo(transactionNo);
        record.setSourceSystem(sourceSystem.getCode());
        record.setSourceBizNo(StringUtils.hasText(command.sourceBizNo()) ? command.sourceBizNo().trim() : null);
        record.setRequestDetail(requestDetail);

        FddEnterpriseAuthDO saved = fddEnterpriseAuthService.insertPending(record);
        log.info("【{}】企业认证已发起, id={}, internalCompany={}, uscc={}, companyId={}, transactionNo={}",
                LOG_BIZ, saved.getId(), internalCompanyName, uscc, binding.companyId(), transactionNo);
        return toEnterpriseResult(saved, false, true, null, "已发起认证，请引导用户完成实名");
    }

    private FddAuthQueryResult initiatePersonAuth(FddPersonAuthQueryCommand command,
                                                  String internalCompanyName,
                                                  String idNumber,
                                                  boolean repeat) {
        String personName = trimRequired(command.personName(), "personName");
        String mobile = trimRequired(command.mobile(), "mobile");
        FddSourceSystemEnum sourceSystem = requireSourceSystem(command.sourceSystem());
        requireCallbackUrl();

        String accountId = ensureAccount(personName, mobile, idNumber);

        FddPersonAuthRequest request = FddPersonAuthRequest.builder()
                .accountId(accountId)
                .tpAccountId(idNumber)
                .verifiedChannel(FddConstants.VERIFIED_CHANNEL_STANDARD)
                .verifiedWay(fddProperties.getPersonVerifiedWay())
                .verifiedType(repeat ? FddConstants.REPEAT_VERIFY : FddConstants.FIRST_VERIFY)
                .name(personName)
                .certType(FddConstants.CERT_TYPE_ID_CARD)
                .idCard(idNumber)
                .notifyUrl(fddProperties.getCallbackUrl())
                .isSendSms(FddConstants.SEND_SMS_NO)
                .otherCertType(FddConstants.OTHER_CERT_TYPE_NO)
                .miniProgram(FddConstants.MINI_PROGRAM_NO)
                .build();

        FddPersonAuthResponse response = fddClient.getPersonAuthUrl(request);
        String authUrl = response.getData().getUrl();
        String transactionNo = response.getData().getTransactionNo();

        Map<String, Object> requestDetail = new LinkedHashMap<>();
        requestDetail.put("accountId", accountId);
        requestDetail.put("request", request);
        requestDetail.put("response", response);

        FddPersonAuthDO record = new FddPersonAuthDO();
        record.setInternalCompanyName(internalCompanyName);
        record.setPersonName(personName);
        record.setIdNumber(idNumber);
        record.setMobile(mobile);
        record.setFddAccountId(accountId);
        record.setAuthUrl(authUrl);
        record.setTransactionNo(transactionNo);
        record.setSourceSystem(sourceSystem.getCode());
        record.setSourceBizNo(StringUtils.hasText(command.sourceBizNo()) ? command.sourceBizNo().trim() : null);
        record.setRequestDetail(requestDetail);

        FddPersonAuthDO saved = fddPersonAuthService.insertPending(record);
        log.info("【{}】个人认证已发起, id={}, internalCompany={}, idNumber={}, accountId={}, transactionNo={}",
                LOG_BIZ, saved.getId(), internalCompanyName, maskIdNumber(idNumber), accountId, transactionNo);
        return toPersonResult(saved, false, true, null, "已发起认证，请引导用户完成实名");
    }

    /**
     * 查询或创建法大大用户，返回 accountId
     */
    private String ensureAccount(String personName, String mobile, String idNumber) {
        FddGetAccountResponse existing = fddClient.getAccount(idNumber, null);
        if (existing != null && existing.hasAccount()) {
            log.info("【{}】法大大用户已存在, accountId={}, idNumber={}",
                    LOG_BIZ, existing.getData().getAccountId(), maskIdNumber(idNumber));
            return existing.getData().getAccountId();
        }
        existing = fddClient.getAccount(null, mobile);
        if (existing != null && existing.hasAccount()) {
            log.info("【{}】法大大用户已存在(按手机号), accountId={}, mobile={}",
                    LOG_BIZ, existing.getData().getAccountId(), maskMobile(mobile));
            return existing.getData().getAccountId();
        }

        FddCreateAccountRequest createRequest = FddCreateAccountRequest.builder()
                .userName(personName)
                .areaCode(AREA_CODE_CN)
                .mobile(mobile)
                .tpAccountId(idNumber)
                .build();
        try {
            FddCreateAccountResponse created = fddClient.createAccount(createRequest);
            if (created == null || !created.isSuccess()) {
                String msg = created == null ? "响应为空"
                        : "code=" + created.getCode() + ", message=" + created.getMessage();
                log.info("【{}】创建用户失败, reason={}, idNumber={}", LOG_BIZ, msg, maskIdNumber(idNumber));
                throw new BizException(ResultCode.INTEGRATION_ERROR, "法大大创建用户失败: " + msg);
            }
            log.info("【{}】创建用户成功, accountId={}, idNumber={}",
                    LOG_BIZ, created.getData().getAccountId(), maskIdNumber(idNumber));
            return created.getData().getAccountId();
        } catch (BizException e) {
            FddGetAccountResponse retry = fddClient.getAccount(idNumber, mobile);
            if (retry != null && retry.hasAccount()) {
                log.info("【{}】创建用户冲突后查询到已有账号, accountId={}", LOG_BIZ, retry.getData().getAccountId());
                return retry.getData().getAccountId();
            }
            throw e;
        }
    }

    /**
     * 查询或创建法大大企业并绑定管理员
     */
    private CompanyBinding ensureCompany(String enterpriseName, String uscc,
                                         String adminName, String adminIdNumber, String adminMobile,
                                         String preferredAccountId) {
        FddGetCompanyResponse existing = fddClient.getCompany(uscc, uscc);
        if (existing != null && existing.hasCompany()) {
            String companyId = existing.getData().getCompanyId();
            String accountId = StringUtils.hasText(preferredAccountId)
                    ? preferredAccountId
                    : ensureAccount(adminName, adminMobile, adminIdNumber);
            log.info("【{}】法大大企业已存在, companyId={}, uscc={}", LOG_BIZ, companyId, uscc);
            return new CompanyBinding(companyId, accountId);
        }

        FddCreateCompanyRequest createRequest = FddCreateCompanyRequest.builder()
                .companyName(enterpriseName)
                .tpOrgId(uscc)
                .adminName(adminName)
                .tpAccountId(adminIdNumber)
                .adminMobile(adminMobile)
                .areaCode(AREA_CODE_CN)
                .build();
        try {
            FddCreateCompanyResponse created = fddClient.createCompany(createRequest);
            if (created == null || !created.isSuccess()) {
                String msg = created == null ? "响应为空"
                        : "code=" + created.getCode() + ", message=" + created.getMessage();
                log.info("【{}】创建企业失败, reason={}, uscc={}", LOG_BIZ, msg, uscc);
                throw new BizException(ResultCode.INTEGRATION_ERROR, "法大大创建企业失败: " + msg);
            }
            String companyId = created.getData().getCompanyId();
            String accountId = StringUtils.hasText(created.getData().getAccountId())
                    ? created.getData().getAccountId()
                    : (StringUtils.hasText(preferredAccountId)
                    ? preferredAccountId
                    : ensureAccount(adminName, adminMobile, adminIdNumber));
            log.info("【{}】创建企业成功, companyId={}, accountId={}, uscc={}",
                    LOG_BIZ, companyId, accountId, uscc);
            return new CompanyBinding(companyId, accountId);
        } catch (BizException e) {
            FddGetCompanyResponse retry = fddClient.getCompany(uscc, uscc);
            if (retry != null && retry.hasCompany()) {
                String accountId = StringUtils.hasText(preferredAccountId)
                        ? preferredAccountId
                        : ensureAccount(adminName, adminMobile, adminIdNumber);
                log.info("【{}】创建企业冲突后查询到已有企业, companyId={}", LOG_BIZ, retry.getData().getCompanyId());
                return new CompanyBinding(retry.getData().getCompanyId(), accountId);
            }
            throw e;
        }
    }

    private FddPersonAuthDO syncPersonIfRemoteCertified(String internalCompanyName, String idNumber,
                                                        String personName, String mobile) {
        try {
            FddGetAccountResponse remote = fddClient.getAccount(idNumber, null);
            if (remote == null || !remote.isCertified()) {
                if (StringUtils.hasText(mobile)) {
                    remote = fddClient.getAccount(null, mobile);
                }
            }
            if (remote == null || !remote.isCertified()) {
                return null;
            }
            FddPersonAuthDO record = new FddPersonAuthDO();
            record.setInternalCompanyName(internalCompanyName);
            record.setPersonName(StringUtils.hasText(personName) ? personName.trim()
                    : remote.getData().getUserName());
            record.setIdNumber(idNumber);
            record.setMobile(StringUtils.hasText(mobile) ? mobile.trim() : remote.getData().getMobile());
            record.setFddAccountId(remote.getData().getAccountId());
            record.setRequestDetail(Map.of("syncFrom", "getAccount", "accountId", remote.getData().getAccountId()));
            return fddPersonAuthService.insertSuccessFromRemote(record);
        } catch (BizException e) {
            log.info("【{}】同步个人认证状态跳过, idNumber={}, reason={}",
                    LOG_BIZ, maskIdNumber(idNumber), e.getMessage());
            return null;
        }
    }

    private FddEnterpriseAuthDO syncEnterpriseIfRemoteCertified(String internalCompanyName, String uscc,
                                                                String enterpriseName) {
        try {
            FddGetCompanyResponse remote = fddClient.getCompany(uscc, uscc);
            if (remote == null || !remote.isCertified()) {
                return null;
            }
            FddEnterpriseAuthDO record = new FddEnterpriseAuthDO();
            record.setInternalCompanyName(internalCompanyName);
            record.setEnterpriseName(StringUtils.hasText(enterpriseName) ? enterpriseName.trim()
                    : remote.getData().getCompanyName());
            record.setUscc(uscc);
            record.setFddCompanyId(remote.getData().getCompanyId());
            record.setRequestDetail(Map.of("syncFrom", "getCompany", "companyId", remote.getData().getCompanyId()));
            return fddEnterpriseAuthService.insertSuccessFromRemote(record);
        } catch (BizException e) {
            log.info("【{}】同步企业认证状态跳过, uscc={}, reason={}", LOG_BIZ, uscc, e.getMessage());
            return null;
        }
    }

    private void validateInternalCompany(String internalCompanyName) {
        if (!fddProperties.isAllowedInternalCompany(internalCompanyName)) {
            log.info("【{}】内部企业校验失败, internalCompany={}", LOG_BIZ, internalCompanyName);
            throw new BizException(ResultCode.PARAM_INVALID,
                    "internalCompanyName 不在允许列表内: " + internalCompanyName);
        }
    }

    private FddSourceSystemEnum requireSourceSystem(String sourceSystemCode) {
        FddSourceSystemEnum sourceSystem = FddSourceSystemEnum.fromCode(sourceSystemCode);
        if (sourceSystem == null) {
            log.info("【{}】发起认证终止, reason=sourceSystem 非法, value={}", LOG_BIZ, sourceSystemCode);
            throw new BizException(ResultCode.PARAM_INVALID, "sourceSystem 仅允许 CRM 或 OA");
        }
        return sourceSystem;
    }

    private void requireCallbackUrl() {
        if (!StringUtils.hasText(fddProperties.getCallbackUrl())
                || fddProperties.getCallbackUrl().contains("{")) {
            log.info("【{}】发起认证终止, reason=callbackUrl 未配置或仍为占位符", LOG_BIZ);
            throw new BizException(ResultCode.PARAM_INVALID, "法大大回调地址未配置");
        }
    }

    private static String trimRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ResultCode.PARAM_MISSING, fieldName + " 不能为空");
        }
        return value.trim();
    }

    private static String maskIdNumber(String idNumber) {
        if (!StringUtils.hasText(idNumber) || idNumber.length() < 10) {
            return "****";
        }
        return idNumber.substring(0, 6) + "********" + idNumber.substring(idNumber.length() - 4);
    }

    private static String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || mobile.length() < 7) {
            return "****";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
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

    private static FddAuthQueryResult toPersonResult(FddPersonAuthDO record,
                                                     boolean certified,
                                                     boolean needAuth,
                                                     Boolean canRetry,
                                                     String message) {
        return new FddAuthQueryResult(
                certified,
                needAuth,
                record.getAuthStatus(),
                FddAuthTypeEnum.PERSON.getCode(),
                record.getInternalCompanyName(),
                record.getAuthUrl(),
                null,
                null,
                record.getPersonName(),
                record.getIdNumber(),
                record.getMobile(),
                record.getSourceSystem(),
                record.getFailReason(),
                record.getCertifiedAt(),
                canRetry,
                message
        );
    }

    private record CompanyBinding(String companyId, String accountId) {
    }
}
