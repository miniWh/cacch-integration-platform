package com.cacch.integration.dto.oa.request;

import lombok.Data;

/**
 * CAP4 表单元数据联调请求（底层走 export 接口）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaCap4FormMetadataApiRequest {

    /**
     * 无流程表单模板编码（formCode），可空（默认 oa.reg-report.form-code）
     */
    private String formCode;

    /**
     * 表单模板编号（templateCode），可空；未传时使用 formCode
     */
    private String templateCode;

    /**
     * CAP4 操作权限 ID，可空（默认 oa.reg-report.right-id）
     */
    private String rightId;

    /**
     * OA 登录名，可空（默认 oa.default-login-name）
     */
    private String loginName;

    /**
     * 导出起始日期 yyyy-MM-dd，可空
     */
    private String beginDateTime;

    /**
     * 导出截止日期 yyyy-MM-dd，可空
     */
    private String endDateTime;

    /**
     * 指定主表数据 ID，可空
     */
    private Long dataId;

    /**
     * 页码，可空（默认 1）
     */
    private Integer page;

    /**
     * 每页条数，可空（默认 1）
     */
    private Integer pageSize;
}
