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
        String personName = trimRequired(command.personName(), "personName（企业联系人）");
        String mobile = trimRequired(command.mobile(), "mobile（企业联系人）");
        validateInternalCompany(internalCompanyName);

        // 企业认证前置：联系人须已个人实名（按内部企业+姓名+手机号，避免仅姓名重名）
        FddPersonAuthDO contact = fddPersonAuthService.findSuccessByContact(
                internalCompanyName, personName, mobile);
        if (contact == null) {
            log.info("【{}】企业认证终止, reason=联系人未个人实名, internalCompany={}, personName={}, mobile={}",
                    LOG_BIZ, internalCompanyName, personName, maskMobile(mobile));
            return new FddAuthQueryResult(
                    false, false, null, FddAuthTypeEnum.ENTERPRISE.getCode(),
                    internalCompanyName, null, command.enterpriseName(), uscc,
                    personName, null, mobile, null, null, null, true,
                    "联系人未完成个人实名认证，无法进行企业认证");
        }

        FddEnterpriseAuthDO success = fddEnterpriseAuthService.findSuccess(internalCompanyName, uscc);
        if (success != null) {
            log.info("【{}】企业认证已通过, internalCompany={}, uscc={}", LOG_BIZ, internalCompanyName, uscc);
            return toEnterpriseResult(success, true, false, null, null, personName, contact.getIdNumber(), mobile);
        }

        FddEnterpriseAuthDO synced = syncEnterpriseIfRemoteCertified(
                internalCompanyName, uscc, command.enterpriseName(), command.sourceSystem(), command.sourceBizNo());
        if (synced != null) {
            log.info("【{}】企业认证法大大侧已通过并同步本地, internalCompany={}, uscc={}, fddCompanyId={}",
                    LOG_BIZ, internalCompanyName, uscc, synced.getFddCompanyId());
            return toEnterpriseResult(synced, true, false, null, null, personName, contact.getIdNumber(), mobile);
        }

        FddEnterpriseAuthDO pending = fddEnterpriseAuthService.findLatestPending(internalCompanyName, uscc);
        if (pending != null) {
            log.info("【{}】企业认证处理中, internalCompany={}, uscc={}, id={}",
                    LOG_BIZ, internalCompanyName, uscc, pending.getId());
            return toEnterpriseResult(pending, false, true, null, "认证处理中",
                    personName, contact.getIdNumber(), mobile);
        }

        FddEnterpriseAuthDO failed = fddEnterpriseAuthService.findLatestFailed(internalCompanyName, uscc);
        boolean autoAuth = command.autoAuth() == null || command.autoAuth();
        if (!autoAuth) {
            if (failed != null) {
                log.info("【{}】企业认证仅查询返回 FAILED, internalCompany={}, uscc={}",
                        LOG_BIZ, internalCompanyName, uscc);
                return toEnterpriseResult(failed, false, false, true, failed.getFailReason(),
                        personName, contact.getIdNumber(), mobile);
            }
            log.info("【{}】企业认证无记录且 autoAuth=false, internalCompany={}, uscc={}",
                    LOG_BIZ, internalCompanyName, uscc);
            return new FddAuthQueryResult(
                    false, false, null, FddAuthTypeEnum.ENTERPRISE.getCode(),
                    internalCompanyName, null, command.enterpriseName(), uscc,
                    personName, contact.getIdNumber(), mobile, null, null, null, true, "需要发起实名认证");
        }

        return initiateEnterpriseAuth(command, internalCompanyName, uscc, contact, failed != null
                || fddEnterpriseAuthService.hasFailedHistory(internalCompanyName, uscc));
    }

    @Override
    public FddAuthQueryResult queryOrAuthPerson(FddPersonAuthQueryCommand command) {
        if (command == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "个人认证查询请求不能为空");
        }
        String internalCompanyName = trimRequired(command.internalCompanyName(), "internalCompanyName");
        String idNumber = trimRequired(command.idNumber(), "idNumber");
        String mobile = trimRequired(command.mobile(), "mobile");
        validateInternalCompany(internalCompanyName);

        FddPersonAuthDO success = fddPersonAuthService.findSuccess(internalCompanyName, idNumber, mobile);
        if (success != null) {
            log.info("【{}】个人认证已通过, internalCompany={}, idNumber={}, mobile={}",
                    LOG_BIZ, internalCompanyName, maskIdNumber(idNumber), maskMobile(mobile));
            return toPersonResult(success, true, false, null, null);
        }

        FddPersonAuthDO synced = syncPersonIfRemoteCertified(
                internalCompanyName, idNumber, command.personName(), mobile,
                command.sourceSystem(), command.sourceBizNo());
        if (synced != null) {
            log.info("【{}】个人认证法大大侧已通过并同步本地, internalCompany={}, idNumber={}, mobile={}, fddAccountId={}",
                    LOG_BIZ, internalCompanyName, maskIdNumber(idNumber), maskMobile(mobile), synced.getFddAccountId());
            return toPersonResult(synced, true, false, null, null);
        }

        FddPersonAuthDO pending = fddPersonAuthService.findLatestPending(internalCompanyName, idNumber, mobile);
        if (pending != null) {
            log.info("【{}】个人认证处理中, internalCompany={}, idNumber={}, mobile={}, id={}",
                    LOG_BIZ, internalCompanyName, maskIdNumber(idNumber), maskMobile(mobile), pending.getId());
            return toPersonResult(pending, false, true, null, "认证处理中");
        }

        FddPersonAuthDO failed = fddPersonAuthService.findLatestFailed(internalCompanyName, idNumber, mobile);
        boolean autoAuth = command.autoAuth() == null || command.autoAuth();
        if (!autoAuth) {
            if (failed != null) {
                log.info("【{}】个人认证仅查询返回 FAILED, internalCompany={}, idNumber={}, mobile={}",
                        LOG_BIZ, internalCompanyName, maskIdNumber(idNumber), maskMobile(mobile));
                return toPersonResult(failed, false, false, true, failed.getFailReason());
            }
            log.info("【{}】个人认证无记录且 autoAuth=false, internalCompany={}, idNumber={}, mobile={}",
                    LOG_BIZ, internalCompanyName, maskIdNumber(idNumber), maskMobile(mobile));
            return new FddAuthQueryResult(
                    false, false, null, FddAuthTypeEnum.PERSON.getCode(),
                    internalCompanyName, null, null, null,
                    command.personName(), idNumber, mobile,
                    null, null, null, true, "需要发起实名认证");
        }

        return initiatePersonAuth(command, internalCompanyName, idNumber, mobile, failed != null
                || fddPersonAuthService.hasFailedHistory(internalCompanyName, idNumber, mobile));
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
                                                      FddPersonAuthDO contact,
                                                      boolean repeat) {
        String enterpriseName = trimRequired(command.enterpriseName(), "enterpriseName");
        String adminName = contact.getPersonName();
        String adminIdNumber = contact.getIdNumber();
        String adminMobile = contact.getMobile();
        // 发起时可额外传 idNumber：须与已实名联系人一致
        if (StringUtils.hasText(command.idNumber())
                && !command.idNumber().trim().equals(adminIdNumber)) {
            log.info("【{}】企业认证终止, reason=联系人身份证与已实名记录不一致, requestIdNumber={}, contactIdNumber={}",
                    LOG_BIZ, maskIdNumber(command.idNumber()), maskIdNumber(adminIdNumber));
            throw new BizException(ResultCode.PARAM_INVALID,
                    "idNumber 与已实名联系人（姓名+手机号）不一致");
        }
        FddSourceSystemEnum sourceSystem = requireSourceSystem(command.sourceSystem());
        requireCallbackUrl();

        CompanyBinding binding = ensureCompany(enterpriseName, uscc, adminName, adminIdNumber, adminMobile,
                contact.getFddAccountId());

        // companyId 与 tpOrgId 二选一；已创建/查询到企业后只传 companyId、accountId
        FddEnterpriseAuthRequest request = FddEnterpriseAuthRequest.builder()
                .companyId(binding.companyId())
                .accountId(binding.accountId())
                .verifiedChannel(FddConstants.VERIFIED_CHANNEL_STANDARD)
                .verifiedWay(fddProperties.getEnterpriseVerifiedWay())
                .isRepeatVerified(repeat ? FddConstants.REPEAT_VERIFY : FddConstants.FIRST_VERIFY)
                .companyInfoDTO(FddEnterpriseAuthRequest.CompanyInfoDTO.builder()
                        .companyName(enterpriseName)
                        .creditCode(uscc)
                        .build())
                .applicationType(FddConstants.APPLICATION_TYPE_ALL)
                .notifyUrl(fddProperties.getCallbackUrl())
                .isSendSms(FddConstants.SEND_SMS_YES)
                .pageModify(FddConstants.PAGE_MODIFY_FORBIDDEN)
                .build();

        FddEnterpriseAuthResponse response = fddClient.getEnterpriseAuthUrl(request);
        String authUrl = response.getData().getUrl();
        String transactionNo = response.getData().getTransactionNo();

        Map<String, Object> requestDetail = new LinkedHashMap<>();
        requestDetail.put("createOrGetCompany", Map.of(
                "companyId", binding.companyId(),
                "accountId", binding.accountId()));
        requestDetail.put("contact", Map.of(
                "personName", adminName,
                "mobile", adminMobile,
                "idNumber", adminIdNumber));
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
        return toEnterpriseResult(saved, false, true, null, "已发起认证，请引导用户完成实名",
                adminName, adminIdNumber, adminMobile);
    }

    private FddAuthQueryResult initiatePersonAuth(FddPersonAuthQueryCommand command,
                                                  String internalCompanyName,
                                                  String idNumber,
                                                  String mobile,
                                                  boolean repeat) {
        String personName = trimRequired(command.personName(), "personName");
        FddSourceSystemEnum sourceSystem = requireSourceSystem(command.sourceSystem());
        requireCallbackUrl();

        String accountId = ensureAccount(personName, mobile, idNumber);

        // accountId 与 tpAccountId 法大大要求二选一；已创建/查询到用户后只传 accountId
        FddPersonAuthRequest request = FddPersonAuthRequest.builder()
                .accountId(accountId)
                .verifiedChannel(FddConstants.VERIFIED_CHANNEL_STANDARD)
                .verifiedWay(fddProperties.getPersonVerifiedWay())
                .verifiedType(repeat ? FddConstants.REPEAT_VERIFY : FddConstants.FIRST_VERIFY)
                .name(personName)
                .certType(FddConstants.CERT_TYPE_ID_CARD)
                .idCard(idNumber)
                .notifyUrl(fddProperties.getCallbackUrl())
                .isSendSms(FddConstants.SEND_SMS_YES)
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
     * 查询或创建法大大用户，返回 accountId。
     * <p>优先按手机号匹配；按身份证命中时须与请求手机号一致，否则视为本业务键下无可用账号。</p>
     */
    private String ensureAccount(String personName, String mobile, String idNumber) {
        FddGetAccountResponse byMobile = fddClient.getAccount(null, mobile);
        if (byMobile != null && byMobile.hasAccount()) {
            log.info("【{}】法大大用户已存在(按手机号), accountId={}, mobile={}",
                    LOG_BIZ, byMobile.firstAccount().getAccountId(), maskMobile(mobile));
            return byMobile.firstAccount().getAccountId();
        }

        FddGetAccountResponse byId = fddClient.getAccount(idNumber, null);
        if (byId != null && byId.hasAccount()) {
            String remoteMobile = byId.firstAccount().getMobile();
            if (mobileEquals(mobile, remoteMobile)) {
                log.info("【{}】法大大用户已存在(按身份证且手机号一致), accountId={}, idNumber={}",
                        LOG_BIZ, byId.firstAccount().getAccountId(), maskIdNumber(idNumber));
                return byId.firstAccount().getAccountId();
            }
            log.info("【{}】法大大存在同身份证账号但手机号不一致, idNumber={}, requestMobile={}, remoteMobile={}, 将尝试创建新账号",
                    LOG_BIZ, maskIdNumber(idNumber), maskMobile(mobile), maskMobile(remoteMobile));
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
                log.info("【{}】创建用户失败, reason={}, idNumber={}, mobile={}",
                        LOG_BIZ, msg, maskIdNumber(idNumber), maskMobile(mobile));
                throw new BizException(ResultCode.INTEGRATION_ERROR, "法大大创建用户失败: " + msg);
            }
            log.info("【{}】创建用户成功, accountId={}, idNumber={}, mobile={}",
                    LOG_BIZ, created.getData().getAccountId(), maskIdNumber(idNumber), maskMobile(mobile));
            return created.getData().getAccountId();
        } catch (BizException e) {
            FddGetAccountResponse retry = fddClient.getAccount(null, mobile);
            if (retry != null && retry.hasAccount()) {
                log.info("【{}】创建用户冲突后按手机号查询到已有账号, accountId={}",
                        LOG_BIZ, retry.firstAccount().getAccountId());
                return retry.firstAccount().getAccountId();
            }
            throw e;
        }
    }

    /**
     * 查询或创建法大大企业并绑定管理员。
     * <p>同一企业不可重复创建，但可重复认证：createCompany 返回 22033（名称已存在）时，
     * 通过 getCompany 取 companyId 后跳过创建，直接进入认证。</p>
     */
    private CompanyBinding ensureCompany(String enterpriseName, String uscc,
                                         String adminName, String adminIdNumber, String adminMobile,
                                         String preferredAccountId) {
        FddGetCompanyResponse existing = findExistingCompany(enterpriseName, uscc);
        if (existing != null && existing.hasCompany()) {
            String companyId = existing.firstCompany().getCompanyId();
            String accountId = resolveCompanyAccountId(preferredAccountId, null,
                    adminName, adminMobile, adminIdNumber);
            log.info("【{}】法大大企业已存在, companyId={}, uscc={}, enterpriseName={}",
                    LOG_BIZ, companyId, uscc, enterpriseName);
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
        FddCreateCompanyResponse created = fddClient.createCompany(createRequest);
        if (created != null && created.isSuccess()) {
            String companyId = created.getData().getCompanyId();
            String accountId = resolveCompanyAccountId(preferredAccountId, created.getData().getAccountId(),
                    adminName, adminMobile, adminIdNumber);
            log.info("【{}】创建企业成功, companyId={}, accountId={}, uscc={}",
                    LOG_BIZ, companyId, accountId, uscc);
            return new CompanyBinding(companyId, accountId);
        }

        Integer failCode = created == null ? null : created.getCode();
        String failMsg = created == null ? "响应为空"
                : "code=" + created.getCode() + ", message=" + created.getMessage();
        log.info("【{}】创建企业未成功, reason={}, uscc={}, enterpriseName={}",
                LOG_BIZ, failMsg, uscc, enterpriseName);

        // 企业名称已存在等冲突：查询复用 companyId，跳过创建直接认证
        FddGetCompanyResponse retry = findExistingCompany(enterpriseName, uscc);
        if (retry != null && retry.hasCompany()) {
            String companyId = retry.firstCompany().getCompanyId();
            String accountId = resolveCompanyAccountId(preferredAccountId, null,
                    adminName, adminMobile, adminIdNumber);
            log.info("【{}】企业已存在跳过创建并复用 companyId, failCode={}, companyId={}, uscc={}",
                    LOG_BIZ, failCode, companyId, uscc);
            return new CompanyBinding(companyId, accountId);
        }

        if (failCode != null && failCode == FddConstants.CODE_COMPANY_NAME_EXISTS) {
            log.info("【{}】创建企业终止, reason=名称已存在但 getCompany 未查到, uscc={}, enterpriseName={}",
                    LOG_BIZ, uscc, enterpriseName);
        }
        throw new BizException(ResultCode.INTEGRATION_ERROR, "法大大创建企业失败: " + failMsg);
    }

    /**
     * 按单条件依次查询已存在企业（creditNo → tpOrgId → companyName）。
     * <p>法大大 getCompany 多参数为 AND，禁止同时传多个条件以免查不到。
     * 单次查询失败不阻断后续条件。</p>
     *
     * @param enterpriseName 外部企业名称
     * @param uscc           统一社会信用代码
     * @return 命中的企业响应；均未命中返回 null
     */
    private FddGetCompanyResponse findExistingCompany(String enterpriseName, String uscc) {
        if (StringUtils.hasText(uscc)) {
            FddGetCompanyResponse byCredit = tryGetCompany(null, null, uscc.trim(), null, "creditNo");
            if (byCredit != null && byCredit.hasCompany()) {
                return byCredit;
            }
            FddGetCompanyResponse byTpOrgId = tryGetCompany(null, null, null, uscc.trim(), "tpOrgId");
            if (byTpOrgId != null && byTpOrgId.hasCompany()) {
                return byTpOrgId;
            }
        }
        if (StringUtils.hasText(enterpriseName)) {
            FddGetCompanyResponse byName = tryGetCompany(null, enterpriseName.trim(), null, null, "companyName");
            if (byName != null && byName.hasCompany()) {
                return byName;
            }
        }
        return null;
    }

    /**
     * 单条件查询企业，失败时记日志并返回 null（不抛出，避免阻断后续条件）
     */
    private FddGetCompanyResponse tryGetCompany(String companyId, String companyName,
                                                String creditNo, String tpOrgId, String byField) {
        try {
            return fddClient.getCompany(companyId, companyName, creditNo, tpOrgId);
        } catch (BizException e) {
            log.info("【{}】getCompany 单条件查询跳过, by={}, reason={}", LOG_BIZ, byField, e.getMessage());
            return null;
        }
    }

    private String resolveCompanyAccountId(String preferredAccountId, String createdAccountId,
                                           String adminName, String adminMobile, String adminIdNumber) {
        if (StringUtils.hasText(createdAccountId)) {
            return createdAccountId;
        }
        if (StringUtils.hasText(preferredAccountId)) {
            return preferredAccountId;
        }
        return ensureAccount(adminName, adminMobile, adminIdNumber);
    }

    private FddPersonAuthDO syncPersonIfRemoteCertified(String internalCompanyName, String idNumber,
                                                        String personName, String mobile,
                                                        String sourceSystem, String sourceBizNo) {
        if (!StringUtils.hasText(mobile)) {
            log.info("【{}】同步个人认证跳过, reason=mobile 为空, idNumber={}", LOG_BIZ, maskIdNumber(idNumber));
            return null;
        }
        try {
            // 业务键含手机号：优先按手机号查；仅当法大大账号手机号与请求一致时才同步 SUCCESS
            FddGetAccountResponse remote = fddClient.getAccount(null, mobile);
            if (remote == null || !remote.isCertified()) {
                remote = fddClient.getAccount(idNumber, null);
                if (remote != null && remote.hasAccount()
                        && !mobileEquals(mobile, remote.firstAccount().getMobile())) {
                    log.info("【{}】同步个人认证跳过, reason=法大大账号手机号与请求不一致, idNumber={}, requestMobile={}, remoteMobile={}",
                            LOG_BIZ, maskIdNumber(idNumber), maskMobile(mobile),
                            maskMobile(remote.firstAccount().getMobile()));
                    return null;
                }
            }
            if (remote == null || !remote.isCertified()) {
                return null;
            }
            if (!mobileEquals(mobile, remote.firstAccount().getMobile())
                    && StringUtils.hasText(remote.firstAccount().getMobile())) {
                log.info("【{}】同步个人认证跳过, reason=手机号不一致, idNumber={}, requestMobile={}, remoteMobile={}",
                        LOG_BIZ, maskIdNumber(idNumber), maskMobile(mobile),
                        maskMobile(remote.firstAccount().getMobile()));
                return null;
            }
            FddGetAccountResponse.FddAccountData account = remote.firstAccount();
            String resolvedName = StringUtils.hasText(personName) ? personName.trim() : account.getUserName();
            if (!StringUtils.hasText(resolvedName)) {
                resolvedName = "未知";
            }
            FddPersonAuthDO record = new FddPersonAuthDO();
            record.setInternalCompanyName(internalCompanyName);
            record.setPersonName(resolvedName);
            record.setIdNumber(idNumber);
            record.setMobile(mobile.trim());
            record.setFddAccountId(account.getAccountId());
            record.setSourceSystem(resolveSyncSourceSystem(sourceSystem));
            record.setSourceBizNo(StringUtils.hasText(sourceBizNo) ? sourceBizNo.trim() : null);
            record.setRequestDetail(Map.of("syncFrom", "getAccount", "accountId", account.getAccountId()));
            return fddPersonAuthService.insertSuccessFromRemote(record);
        } catch (BizException e) {
            log.info("【{}】同步个人认证状态跳过, idNumber={}, mobile={}, reason={}",
                    LOG_BIZ, maskIdNumber(idNumber), maskMobile(mobile), e.getMessage());
            return null;
        }
    }

    private FddEnterpriseAuthDO syncEnterpriseIfRemoteCertified(String internalCompanyName, String uscc,
                                                                String enterpriseName,
                                                                String sourceSystem, String sourceBizNo) {
        try {
            FddGetCompanyResponse remote = findExistingCompany(enterpriseName, uscc);
            if (remote == null || !remote.isCertified()) {
                return null;
            }
            FddGetCompanyResponse.FddCompanyData company = remote.firstCompany();
            String resolvedName = StringUtils.hasText(enterpriseName) ? enterpriseName.trim()
                    : company.getCompanyName();
            if (!StringUtils.hasText(resolvedName)) {
                resolvedName = "未知企业";
            }
            FddEnterpriseAuthDO record = new FddEnterpriseAuthDO();
            record.setInternalCompanyName(internalCompanyName);
            record.setEnterpriseName(resolvedName);
            record.setUscc(uscc);
            record.setFddCompanyId(company.getCompanyId());
            record.setSourceSystem(resolveSyncSourceSystem(sourceSystem));
            record.setSourceBizNo(StringUtils.hasText(sourceBizNo) ? sourceBizNo.trim() : null);
            record.setRequestDetail(Map.of("syncFrom", "getCompany", "companyId", company.getCompanyId()));
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
        FddSourceSystemEnum sourceSystem = FddSourceSystemEnum.fromInitiateCode(sourceSystemCode);
        if (sourceSystem == null) {
            log.info("【{}】发起认证终止, reason=sourceSystem 非法, value={}", LOG_BIZ, sourceSystemCode);
            throw new BizException(ResultCode.PARAM_INVALID, "sourceSystem 仅允许 CRM 或 OA");
        }
        return sourceSystem;
    }

    /**
     * 同步落库审计来源：优先用请求中的 CRM/OA，否则标记为 SYNC
     */
    private static String resolveSyncSourceSystem(String sourceSystemCode) {
        FddSourceSystemEnum initiate = FddSourceSystemEnum.fromInitiateCode(sourceSystemCode);
        if (initiate != null) {
            return initiate.getCode();
        }
        return FddSourceSystemEnum.SYNC.getCode();
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

    private static boolean mobileEquals(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return left.trim().equals(right.trim());
    }

    private static FddAuthQueryResult toEnterpriseResult(FddEnterpriseAuthDO record,
                                                         boolean certified,
                                                         boolean needAuth,
                                                         Boolean canRetry,
                                                         String message,
                                                         String personName,
                                                         String idNumber,
                                                         String mobile) {
        return new FddAuthQueryResult(
                certified,
                needAuth,
                record.getAuthStatus(),
                FddAuthTypeEnum.ENTERPRISE.getCode(),
                record.getInternalCompanyName(),
                record.getAuthUrl(),
                record.getEnterpriseName(),
                record.getUscc(),
                personName,
                idNumber,
                mobile,
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
