package com.cacch.integration.controller.oa;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.dto.oa.request.OaRegReportAttachmentBindRequest;
import com.cacch.integration.dto.oa.vo.OaRegReportAttachmentBindVO;
import com.cacch.integration.integration.oa.client.dto.OaRegReportAttachmentBindResult;
import com.cacch.integration.service.oa.api.IOaRegReportOpenApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 国内登记报告 OA 联调接口（上传 + CAP4 附件绑定）
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping("/api/v1/oa/reg-reports")
@RequiredArgsConstructor
public class OaRegReportOpenApiController {

    private final IOaRegReportOpenApiService oaRegReportOpenApiService;

    /**
     * 上传文件并通过 CAP4 batch-update 绑定至资料列表子表 field0218
     *
     * @param file          multipart 文件，表单字段名 {@code file}
     * @param formMainId    formmain_4070.id
     * @param subRowId      formson_5464.id
     * @param subReference  已有 field0218 的 subReference，可空（空则自动生成）
     * @param formCode      无流程表单模板编码，可空
     * @param rightId       CAP4 操作权限 ID，可空
     * @param loginName     OA 登录名，可空
     * @param sort          附件排序，可空（默认 1）
     * @param doTrigger     是否执行触发器，可空
     * @return 上传与绑定结果
     * @throws IOException 读取 multipart 文件失败时抛出
     */
    @PostMapping(value = "/attachments/upload-and-bind", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<OaRegReportAttachmentBindVO> uploadAndBindAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam("formMainId") Long formMainId,
            @RequestParam("subRowId") Long subRowId,
            @RequestParam(required = false) String subReference,
            @RequestParam(required = false) String formCode,
            @RequestParam(required = false) String rightId,
            @RequestParam(required = false) String loginName,
            @RequestParam(required = false) Integer sort,
            @RequestParam(required = false) Boolean doTrigger) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_MISSING, "file 不能为空");
        }
        OaRegReportAttachmentBindResult result = oaRegReportOpenApiService.uploadAndBindAttachment(
                file.getBytes(),
                file.getOriginalFilename(),
                file.getContentType(),
                formMainId,
                subRowId,
                subReference,
                formCode,
                rightId,
                loginName,
                sort,
                doTrigger);
        return Result.success(toVo(result));
    }

    /**
     * 将已上传的 fileUrl 通过 CAP4 batch-update 绑定至资料列表子表 field0218
     *
     * @param request 绑定参数，含 fileUrl、formMainId、subRowId 等
     * @return 绑定结果
     */
    @PostMapping("/attachments/bind")
    public Result<OaRegReportAttachmentBindVO> bindAttachment(
            @Valid @RequestBody OaRegReportAttachmentBindRequest request) {
        OaRegReportAttachmentBindResult result = oaRegReportOpenApiService.bindAttachment(
                request.getFileUrl(),
                request.getFormMainId(),
                request.getSubRowId(),
                request.getSubReference(),
                request.getFormCode(),
                request.getRightId(),
                request.getLoginName(),
                request.getSort(),
                request.getDoTrigger());
        return Result.success(toVo(result));
    }

    private static OaRegReportAttachmentBindVO toVo(OaRegReportAttachmentBindResult result) {
        return new OaRegReportAttachmentBindVO(
                result.fileUrl(),
                result.fileName(),
                result.subReference(),
                result.uploadRawResponse(),
                result.batchUpdateResponse());
    }
}
