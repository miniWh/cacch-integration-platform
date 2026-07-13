package com.cacch.integration.controller.oa;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.oa.request.OaOrgMemberByCodeRequest;
import com.cacch.integration.dto.oa.request.OaProcessStartApiRequest;
import com.cacch.integration.dto.oa.request.OaTokenRequest;
import com.cacch.integration.dto.oa.vo.OaTokenVO;
import com.cacch.integration.integration.oa.client.dto.OaOrgMember;
import com.cacch.integration.integration.oa.client.dto.OaProcessStartRequest;
import com.cacch.integration.service.oa.api.IOaOpenApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * 致远 OA REST 联调接口（手动触发）
 *
 * <p>凭证取自配置 {@code oa.rest-user-name}/{@code oa.rest-password}，调用方无需传 REST 密码。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping("/api/v1/oa/open-api")
@RequiredArgsConstructor
public class OaOpenApiController {

    private final IOaOpenApiService oaOpenApiService;

    /**
     * 获取 Rest Token（Redis 缓存）
     *
     * @param request 可含 loginName，可空 body
     * @return Token
     */
    @PostMapping("/token")
    public Result<OaTokenVO> getToken(@RequestBody(required = false) OaTokenRequest request) {
        String loginName = request == null ? null : request.getLoginName();
        return Result.success(new OaTokenVO(oaOpenApiService.getToken(loginName)));
    }

    /**
     * 清除 Token 缓存
     *
     * @param request 可含 loginName
     * @return 无数据成功响应
     */
    @PostMapping("/token/evict")
    public Result<Void> evictToken(@RequestBody(required = false) OaTokenRequest request) {
        String loginName = request == null ? null : request.getLoginName();
        oaOpenApiService.evictToken(loginName);
        return Result.success(null);
    }

    /**
     * 按人员编码取人员（原始 JSON）
     *
     * @param request 含 code 与可选分页 / loginName
     * @return 致远原始响应
     */
    @PostMapping("/org-members/query-by-code")
    public Result<JsonNode> queryOrgMembersByCode(@Valid @RequestBody OaOrgMemberByCodeRequest request) {
        return Result.success(oaOpenApiService.getOrgMembersByCode(
                request.getCode(), request.getPageNo(), request.getPageSize(), request.getLoginName()));
    }

    /**
     * 按人员编码解析首个人员对象（取 id / loginName）
     *
     * @param request 含 code
     * @return 人员；查无时 data 为 null
     */
    @PostMapping("/org-members/resolve-by-code")
    public Result<OaOrgMember> resolveOrgMemberByCode(@Valid @RequestBody OaOrgMemberByCodeRequest request) {
        return Result.success(oaOpenApiService.getOrgMemberByCode(request.getCode(), request.getLoginName()));
    }

    /**
     * 发起表单流程（对齐 CRM_ZYXS_001 / formmain_2817 + formson_2819）
     *
     * @param request 表单字段与可选 loginName
     * @return 致远原始响应
     */
    @PostMapping("/bpm/process/start")
    public Result<JsonNode> startProcess(@Valid @RequestBody OaProcessStartApiRequest request) {
        OaProcessStartRequest startRequest = OaProcessStartRequest.builder()
                .loginName(request.getLoginName())
                .appName(request.getAppName())
                .templateCode(request.getTemplateCode())
                .draft(request.getDraft())
                .formmain2817(request.getFormmain2817())
                .formson2819(request.getFormson2819())
                .attachments(request.getAttachments())
                .build();
        return Result.success(oaOpenApiService.startProcess(startRequest));
    }

    /**
     * 查询流程状态
     *
     * @param flowId    流程实例 ID
     * @param loginName Token 绑定登录名，可选
     * @return 致远原始响应
     */
    @GetMapping("/flow/state/{flowId}")
    public Result<JsonNode> getFlowState(@PathVariable String flowId,
                                         @RequestParam(required = false) String loginName) {
        return Result.success(oaOpenApiService.getFlowState(flowId, loginName));
    }
}
