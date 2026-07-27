package com.cacch.integration.controller.oa;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.dto.oa.request.OaCap4FormMetadataApiRequest;
import com.cacch.integration.dto.oa.request.OaOrgMemberByCodeRequest;
import com.cacch.integration.dto.oa.request.OaProcessStartApiRequest;
import com.cacch.integration.dto.oa.request.OaTokenRequest;
import com.cacch.integration.dto.oa.vo.OaFileUploadVO;
import com.cacch.integration.dto.oa.vo.OaTokenVO;
import com.cacch.integration.integration.oa.client.dto.OaFileUploadResult;
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
import org.springframework.web.multipart.MultipartFile;
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

    /**
     * 上传附件至致远 OA（联调/核实接口可用性）
     *
     * @param file      multipart 文件，表单字段名 {@code file}
     * @param loginName Token 绑定登录名，可空（默认使用配置 {@code oa.default-login-name}）
     * @return 文件 ID（fileUrl）；绑定子表附件请使用 reg-reports 上传并绑定接口
     * @throws java.io.IOException 读取 multipart 文件失败时抛出
     */
    @PostMapping(value = "/attachments/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<OaFileUploadVO> uploadAttachment(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(required = false) String loginName) throws java.io.IOException {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_MISSING, "file 不能为空");
        }
        OaFileUploadResult result = oaOpenApiService.uploadAttachment(
                file.getBytes(),
                file.getOriginalFilename(),
                file.getContentType(),
                loginName);
        return Result.success(new OaFileUploadVO(result.fileUrl(), result.fileName(), result.rawResponse()));
    }

    /**
     * 获取 CAP4 表单元数据（POST，底层调用 {@code /seeyon/rest/cap4/form/soap/export}）
     *
     * @param request 可含 templateCode、rightId、日期范围等；body 可空（使用 yml 默认配置）
     * @return 致远原始响应（元数据见 {@code data.data.definition}）
     */
    @PostMapping("/cap4/form/metadata")
    public Result<JsonNode> getCap4FormMetadata(@RequestBody(required = false) OaCap4FormMetadataApiRequest request) {
        OaCap4FormMetadataApiRequest req = request != null ? request : new OaCap4FormMetadataApiRequest();
        return Result.success(oaOpenApiService.getCap4FormMetadata(
                req.getFormCode(),
                req.getTemplateCode(),
                req.getRightId(),
                req.getLoginName(),
                req.getBeginDateTime(),
                req.getEndDateTime(),
                req.getDataId(),
                req.getPage(),
                req.getPageSize()));
    }

    /**
     * 获取 CAP4 表单元数据（GET 联调，底层走 export）
     *
     * @param formCode      无流程表单模板编码，可空
     * @param templateCode  表单模板编号，可空
     * @param rightId       CAP4 操作权限 ID，可空
     * @param loginName     OA 登录名，可空
     * @param beginDateTime 导出起始日期 yyyy-MM-dd，可空
     * @param endDateTime   导出截止日期 yyyy-MM-dd，可空
     * @param dataId        主表数据 ID，可空
     * @param page          页码，可空
     * @param pageSize      每页条数，可空
     * @return 致远原始响应
     */
    @GetMapping("/cap4/form/metadata")
    public Result<JsonNode> getCap4FormMetadataByQuery(@RequestParam(required = false) String formCode,
                                                       @RequestParam(required = false) String templateCode,
                                                       @RequestParam(required = false) String rightId,
                                                       @RequestParam(required = false) String loginName,
                                                       @RequestParam(required = false) String beginDateTime,
                                                       @RequestParam(required = false) String endDateTime,
                                                       @RequestParam(required = false) Long dataId,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer pageSize) {
        return Result.success(oaOpenApiService.getCap4FormMetadata(
                formCode, templateCode, rightId, loginName,
                beginDateTime, endDateTime, dataId, page, pageSize));
    }
}
