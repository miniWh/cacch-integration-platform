package com.cacch.integration.dto.oa.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 国内登记报告资料附件绑定请求（fileUrl 已上传时使用）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaRegReportAttachmentBindRequest {

    /**
     * REST 上传返回的文件 ID（fileUrl）
     */
    @NotBlank(message = "fileUrl 不能为空")
    private String fileUrl;

    /**
     * formmain_4070.id
     */
    @NotNull(message = "formMainId 不能为空")
    private Long formMainId;

    /**
     * formson_5464.id
     */
    @NotNull(message = "subRowId 不能为空")
    private Long subRowId;

    /**
     * 已有 field0218 的 subReference；为空则自动生成
     */
    private String subReference;

    /**
     * 无流程表单模板编码，可空（使用配置 oa.reg-report.form-code）
     */
    private String formCode;

    /**
     * CAP4 操作权限 ID，可空（使用配置 oa.reg-report.right-id）
     */
    private String rightId;

    /**
     * OA 登录名，可空（使用配置 oa.default-login-name）
     */
    private String loginName;

    /**
     * 附件排序，默认 1
     */
    private Integer sort;

    /**
     * 是否执行触发器，可空（使用配置 oa.reg-report.do-trigger）
     */
    private Boolean doTrigger;
}
