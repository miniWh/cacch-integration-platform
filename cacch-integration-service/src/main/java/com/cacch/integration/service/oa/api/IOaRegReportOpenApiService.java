package com.cacch.integration.service.oa.api;

import com.cacch.integration.integration.oa.client.dto.OaRegReportAttachmentBindResult;

/**
 * 国内登记报告 OA 联调服务（上传 + CAP4 附件绑定）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaRegReportOpenApiService {

    /**
     * 上传文件并通过 CAP4 batch-update 绑定至资料列表子表附件字段
     *
     * @param fileBytes     文件内容，不可为空
     * @param fileName      文件名，不可为空
     * @param contentType   MIME 类型，可空
     * @param formMainId    formmain_4070.id，不可为空
     * @param subRowId      formson_5464.id，不可为空
     * @param subReference  已有 field0218 值，可空（空则自动生成 subReference）
     * @param formCode      无流程表单模板编码，可空（使用配置 {@code oa.reg-report.form-code}）
     * @param rightId       CAP4 操作权限 ID，可空（使用配置 {@code oa.reg-report.right-id}）
     * @param loginName     OA 登录名，可空（使用配置 {@code oa.default-login-name}）
     * @param sort          附件排序，可空（默认 1）
     * @param doTrigger     是否执行触发器，可空（使用配置 {@code oa.reg-report.do-trigger}）
     * @return 上传与绑定结果
     */
    OaRegReportAttachmentBindResult uploadAndBindAttachment(byte[] fileBytes,
                                                           String fileName,
                                                           String contentType,
                                                           Long formMainId,
                                                           Long subRowId,
                                                           String subReference,
                                                           String formCode,
                                                           String rightId,
                                                           String loginName,
                                                           Integer sort,
                                                           Boolean doTrigger);

    /**
     * 将已上传的 fileUrl 通过 CAP4 batch-update 绑定至资料列表子表附件字段
     *
     * @param fileUrl       REST 上传返回的文件 ID，不可为空
     * @param formMainId    formmain_4070.id，不可为空
     * @param subRowId      formson_5464.id，不可为空
     * @param subReference  已有 field0218 值，可空（空则自动生成 subReference）
     * @param formCode      无流程表单模板编码，可空
     * @param rightId       CAP4 操作权限 ID，可空
     * @param loginName     OA 登录名，可空
     * @param sort          附件排序，可空（默认 1）
     * @param doTrigger     是否执行触发器，可空
     * @return 绑定结果（uploadRawResponse 为 null）
     */
    OaRegReportAttachmentBindResult bindAttachment(String fileUrl,
                                                   Long formMainId,
                                                   Long subRowId,
                                                   String subReference,
                                                   String formCode,
                                                   String rightId,
                                                   String loginName,
                                                   Integer sort,
                                                   Boolean doTrigger);
}
