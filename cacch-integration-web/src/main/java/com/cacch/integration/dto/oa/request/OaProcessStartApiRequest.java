package com.cacch.integration.dto.oa.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 发起 OA 表单流程联调请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaProcessStartApiRequest {

    /**
     * 发起人登录名（获取 Token 的 loginName），可空
     */
    private String loginName;

    /**
     * 应用类型，默认 collaboration
     */
    private String appName;

    /**
     * 模板编号，默认配置 oa.template-code
     */
    private String templateCode;

    /**
     * 0=发送，1=待发
     */
    private String draft;

    /**
     * 主表字段 formmain_2817
     */
    private Map<String, Object> formmain2817;

    /**
     * 明细子表单行 formson_2819
     */
    private Map<String, Object> formson2819;

    /**
     * 附件 ID 列表
     */
    private List<Long> attachments;
}
