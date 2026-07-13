package com.cacch.integration.integration.oa.client.dto;

import com.cacch.integration.common.constant.oa.OaConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 致远 OA 发起表单流程请求（对齐 Seeyon bpm/process/start 结构）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OaProcessStartRequest {

    /**
     * 绑定发起人登录名（获取 Token 时的 loginName）；可空则用配置默认值
     */
    private String loginName;

    /**
     * 应用类型，默认 collaboration
     */
    private String appName;

    /**
     * 模板编号；可空则用配置 {@code oa.template-code}
     */
    private String templateCode;

    /**
     * 是否待发：0=发送，1=待发
     */
    private String draft;

    /**
     * 主表字段（formmain_2817）
     */
    private Map<String, Object> formmain2817;

    /**
     * 明细子表单行字段（formson_2819）；发起时封装为单元素数组
     */
    private Map<String, Object> formson2819;

    /**
     * 附件 ID 列表；可空
     */
    private List<Long> attachments;

    /**
     * 组装致远官方嵌套报文
     *
     * @param defaultTemplateCode 配置默认模板编号
     * @return 可直接 POST 的请求体
     */
    public Map<String, Object> toSeeyonBody(String defaultTemplateCode) {
        String resolvedAppName = blankToDefault(appName, OaConstants.APP_NAME_COLLABORATION);
        String resolvedDraft = blankToDefault(draft, OaConstants.DRAFT_SEND);
        String resolvedTemplate = blankToDefault(templateCode, defaultTemplateCode);

        Map<String, Object> formData = new LinkedHashMap<>();
        if (formmain2817 != null) {
            formData.put(OaConstants.FORM_MAIN, formmain2817);
        }
        if (formson2819 != null) {
            formData.put(OaConstants.FORM_SON_DETAIL, List.of(formson2819));
        }

        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("templateCode", resolvedTemplate);
        inner.put("draft", resolvedDraft);
        inner.put("attachments", attachments != null ? attachments : new ArrayList<Long>());
        inner.put("data", formData);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appName", resolvedAppName);
        body.put("data", inner);
        return body;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
