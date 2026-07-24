package com.cacch.integration.service.oa.api.impl;

import com.cacch.integration.common.constant.oa.OaConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.oa.client.OaClient;
import com.cacch.integration.integration.oa.client.dto.OaFileUploadResult;
import com.cacch.integration.integration.oa.client.dto.OaOrgMember;
import com.cacch.integration.integration.oa.client.dto.OaProcessStartRequest;
import com.cacch.integration.integration.oa.support.OaResponseSupport;
import com.cacch.integration.service.oa.api.IOaOpenApiService;
import com.cacch.integration.service.oa.api.IOaTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * 致远 OA OpenAPI 服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaOpenApiServiceImpl implements IOaOpenApiService {

    private final OaClient oaClient;
    private final IOaTokenService oaTokenService;

    @Override
    public String getToken(String loginName) {
        return oaTokenService.getToken(loginName);
    }

    @Override
    public void evictToken(String loginName) {
        oaTokenService.evictToken(loginName);
    }

    @Override
    public JsonNode getOrgMembersByCode(String code, Integer pageNo, Integer pageSize, String loginName) {
        if (!StringUtils.hasText(code)) {
            log.info("【OaOpenApi】按编码取人员终止, reason=code为空");
            throw new BizException(ResultCode.PARAM_MISSING, "code 不能为空");
        }
        int p = pageNo == null ? OaConstants.DEFAULT_PAGE_NO : pageNo;
        int s = pageSize == null || pageSize < 1 ? OaConstants.DEFAULT_PAGE_SIZE : pageSize;
        String token = oaTokenService.getToken(loginName);
        try {
            return oaClient.getOrgMembersByCode(token, code.trim(), p, s);
        } catch (RestClientException e) {
            log.info("【OaOpenApi】按编码取人员终止, code={}, reason={}", code, e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "致远 OA 按编码取人员失败: " + e.getMessage(), e);
        }
    }

    @Override
    public OaOrgMember getOrgMemberByCode(String code, String loginName) {
        JsonNode raw = getOrgMembersByCode(code, OaConstants.DEFAULT_PAGE_NO, OaConstants.DEFAULT_PAGE_SIZE, loginName);
        OaOrgMember member = OaResponseSupport.firstOrgMember(raw);
        if (member == null || !StringUtils.hasText(member.getId())) {
            log.info("【OaOpenApi】按编码取人员无有效结果, code={}", code);
        } else {
            log.info("【OaOpenApi】按编码取人员成功, code={}, oaUserId={}, loginName={}",
                    code, member.getId(), member.getLoginName());
        }
        return member;
    }

    @Override
    public JsonNode startProcess(OaProcessStartRequest request) {
        if (request == null) {
            log.info("【OaOpenApi】发起表单终止, reason=请求体为空");
            throw new BizException(ResultCode.PARAM_MISSING, "发起表单请求体不能为空");
        }
        String token = oaTokenService.getToken(request.getLoginName());
        try {
            JsonNode response = oaClient.startProcess(token, request);
            log.info("【OaOpenApi】发起表单流程完成, templateCode={}", request.getTemplateCode());
            return response;
        } catch (RestClientException e) {
            log.info("【OaOpenApi】发起表单终止, reason={}", e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "致远 OA 发起表单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public JsonNode getFlowState(String flowId, String loginName) {
        if (!StringUtils.hasText(flowId)) {
            log.info("【OaOpenApi】查询流程状态终止, reason=flowId为空");
            throw new BizException(ResultCode.PARAM_MISSING, "flowId 不能为空");
        }
        String token = oaTokenService.getToken(loginName);
        try {
            return oaClient.getFlowState(token, flowId.trim());
        } catch (RestClientException e) {
            log.info("【OaOpenApi】查询流程状态终止, flowId={}, reason={}", flowId, e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "致远 OA 查询流程状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    public OaFileUploadResult uploadAttachment(byte[] fileBytes, String fileName, String contentType, String loginName) {
        if (fileBytes == null || fileBytes.length == 0) {
            log.info("【OaOpenApi】上传附件终止, reason=文件内容为空");
            throw new BizException(ResultCode.PARAM_MISSING, "上传文件不能为空");
        }
        if (!StringUtils.hasText(fileName)) {
            log.info("【OaOpenApi】上传附件终止, reason=文件名为空");
            throw new BizException(ResultCode.PARAM_MISSING, "文件名不能为空");
        }
        String token = oaTokenService.getToken(loginName);
        try {
            OaFileUploadResult result = oaClient.uploadAttachment(
                    token, fileBytes, fileName.trim(), contentType);
            log.info("【OaOpenApi】上传附件完成, fileName={}, fileUrl={}", result.fileName(), result.fileUrl());
            return result;
        } catch (RestClientException e) {
            log.info("【OaOpenApi】上传附件终止, fileName={}, reason={}", fileName, e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "致远 OA 上传附件失败: " + e.getMessage(), e);
        }
    }
}
