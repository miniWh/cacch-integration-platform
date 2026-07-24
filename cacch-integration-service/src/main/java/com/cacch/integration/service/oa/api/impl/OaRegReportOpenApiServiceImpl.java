package com.cacch.integration.service.oa.api.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.common.config.oa.OaProperties;
import com.cacch.integration.common.config.oa.OaRegReportProperties;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.oa.client.OaClient;
import com.cacch.integration.integration.oa.client.dto.OaCap4BatchUpdateRequest;
import com.cacch.integration.integration.oa.client.dto.OaFileUploadResult;
import com.cacch.integration.integration.oa.client.dto.OaRegReportAttachmentBindResult;
import com.cacch.integration.integration.oa.support.OaResponseSupport;
import com.cacch.integration.service.oa.api.IOaRegReportOpenApiService;
import com.cacch.integration.service.oa.api.IOaTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.Map;

/**
 * 国内登记报告 OA 联调服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaRegReportOpenApiServiceImpl implements IOaRegReportOpenApiService {

    private static final String BIZ = "OaRegReport";

    private final OaClient oaClient;
    private final IOaTokenService oaTokenService;
    private final OaProperties oaProperties;
    private final OaRegReportProperties regReportProperties;

    @Override
    public OaRegReportAttachmentBindResult uploadAndBindAttachment(byte[] fileBytes,
                                                                   String fileName,
                                                                   String contentType,
                                                                   Long formMainId,
                                                                   Long subRowId,
                                                                   String subReference,
                                                                   String formCode,
                                                                   String rightId,
                                                                   String loginName,
                                                                   Integer sort,
                                                                   Boolean doTrigger) {
        if (fileBytes == null || fileBytes.length == 0) {
            log.info("【{}】上传并绑定附件终止, reason=文件内容为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "上传文件不能为空");
        }
        if (!StringUtils.hasText(fileName)) {
            log.info("【{}】上传并绑定附件终止, reason=文件名为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "文件名不能为空");
        }
        String resolvedLogin = resolveLoginName(loginName);
        String token = oaTokenService.getToken(resolvedLogin);
        OaFileUploadResult uploadResult;
        try {
            uploadResult = oaClient.uploadAttachment(token, fileBytes, fileName.trim(), contentType);
        } catch (RestClientException e) {
            log.info("【{}】上传并绑定附件终止, step=upload, reason={}", BIZ, e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "致远 OA 上传附件失败: " + e.getMessage(), e);
        }
        BindContext context = resolveBindContext(formMainId, subRowId, subReference, formCode, rightId,
                resolvedLogin, sort, doTrigger);
        JsonNode batchResponse = executeBatchUpdate(token, uploadResult.fileUrl(), context);
        log.info("【{}】上传并绑定附件完成, formMainId={}, subRowId={}, subReference={}, fileUrl={}",
                BIZ, formMainId, subRowId, context.subReference(), uploadResult.fileUrl());
        return new OaRegReportAttachmentBindResult(
                uploadResult.fileUrl(),
                uploadResult.fileName(),
                context.subReference(),
                uploadResult.rawResponse(),
                batchResponse);
    }

    @Override
    public OaRegReportAttachmentBindResult bindAttachment(String fileUrl,
                                                          Long formMainId,
                                                          Long subRowId,
                                                          String subReference,
                                                          String formCode,
                                                          String rightId,
                                                          String loginName,
                                                          Integer sort,
                                                          Boolean doTrigger) {
        if (!StringUtils.hasText(fileUrl)) {
            log.info("【{}】绑定附件终止, reason=fileUrl为空", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "fileUrl 不能为空");
        }
        String resolvedLogin = resolveLoginName(loginName);
        String token = oaTokenService.getToken(resolvedLogin);
        BindContext context = resolveBindContext(formMainId, subRowId, subReference, formCode, rightId,
                resolvedLogin, sort, doTrigger);
        JsonNode batchResponse = executeBatchUpdate(token, fileUrl.trim(), context);
        log.info("【{}】绑定附件完成, formMainId={}, subRowId={}, subReference={}, fileUrl={}",
                BIZ, formMainId, subRowId, context.subReference(), fileUrl.trim());
        return new OaRegReportAttachmentBindResult(
                fileUrl.trim(),
                null,
                context.subReference(),
                null,
                batchResponse);
    }

    private JsonNode executeBatchUpdate(String token, String fileUrl, BindContext context) {
        Map<String, Object> body = OaCap4BatchUpdateRequest.regReportAttachmentBind(
                context.formCode(),
                context.loginName(),
                context.rightId(),
                context.doTrigger(),
                regReportProperties.getFormMainTable(),
                context.formMainId(),
                regReportProperties.getFormSubTable(),
                context.subRowId(),
                regReportProperties.getAttachmentField(),
                context.subReference(),
                fileUrl,
                context.sort());
        try {
            JsonNode response = oaClient.batchUpdateCap4Form(token, body);
            if (!OaResponseSupport.isCap4BatchUpdateSuccess(response)) {
                String reason = OaResponseSupport.extractCap4BatchUpdateFailureMessage(response);
                log.info("【{}】CAP4 绑定附件终止, formMainId={}, subRowId={}, reason={}",
                        BIZ, context.formMainId(), context.subRowId(), reason);
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "致远 OA CAP4 绑定附件失败: " + (reason != null ? reason : "batch-update 未成功"));
            }
            return response;
        } catch (RestClientException e) {
            log.info("【{}】CAP4 绑定附件终止, formMainId={}, subRowId={}, reason={}",
                    BIZ, context.formMainId(), context.subRowId(), e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "致远 OA CAP4 绑定附件失败: " + e.getMessage(), e);
        }
    }

    private BindContext resolveBindContext(Long formMainId,
                                           Long subRowId,
                                           String subReference,
                                           String formCode,
                                           String rightId,
                                           String loginName,
                                           Integer sort,
                                           Boolean doTrigger) {
        if (formMainId == null || formMainId <= 0) {
            log.info("【{}】绑定附件终止, reason=formMainId无效", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "formMainId 不能为空");
        }
        if (subRowId == null || subRowId <= 0) {
            log.info("【{}】绑定附件终止, reason=subRowId无效", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "subRowId 不能为空");
        }
        if (!StringUtils.hasText(loginName)) {
            log.info("【{}】绑定附件终止, reason=loginName未配置", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING,
                    "loginName 不能为空，请传参或配置 oa.default-login-name");
        }
        String resolvedFormCode = blankToDefault(formCode, regReportProperties.getFormCode());
        String resolvedRightId = blankToDefault(rightId, regReportProperties.getRightId());
        if (!StringUtils.hasText(resolvedFormCode)) {
            log.info("【{}】绑定附件终止, reason=formCode未配置", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING,
                    "formCode 不能为空，请传参或配置 oa.reg-report.form-code");
        }
        if (!StringUtils.hasText(resolvedRightId)) {
            log.info("【{}】绑定附件终止, reason=rightId未配置", BIZ);
            throw new BizException(ResultCode.PARAM_MISSING,
                    "rightId 不能为空，请传参或配置 oa.reg-report.right-id");
        }
        String resolvedSubReference = StringUtils.hasText(subReference)
                ? subReference.trim()
                : String.valueOf(IdWorker.getId());
        int resolvedSort = sort == null || sort < 1 ? 1 : sort;
        boolean resolvedDoTrigger = doTrigger != null ? doTrigger : regReportProperties.isDoTrigger();
        return new BindContext(formMainId, subRowId, resolvedSubReference, resolvedFormCode,
                resolvedRightId, loginName, resolvedSort, resolvedDoTrigger);
    }

    private String resolveLoginName(String loginName) {
        if (StringUtils.hasText(loginName)) {
            return loginName.trim();
        }
        if (StringUtils.hasText(oaProperties.getDefaultLoginName())) {
            return oaProperties.getDefaultLoginName().trim();
        }
        return null;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private record BindContext(
            long formMainId,
            long subRowId,
            String subReference,
            String formCode,
            String rightId,
            String loginName,
            int sort,
            boolean doTrigger
    ) {
    }
}
