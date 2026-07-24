package com.cacch.integration.common.config.oa;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 国内登记报告（formmain_4070 / formson_5464）CAP4 联调配置
 *
 * @author hongfu_zhou@cacch.com
 */
@ConfigurationProperties(prefix = "oa.reg-report")
public class OaRegReportProperties {

    /**
     * 无流程表单模板编码（formCode），由 OA 管理员提供
     */
    private final String formCode;

    /**
     * CAP4 批量更新操作权限 ID（rightId），由 OA 管理员提供
     */
    private final String rightId;

    /**
     * 主表物理名
     */
    private final String formMainTable;

    /**
     * 资料列表子表物理名
     */
    private final String formSubTable;

    /**
     * 资料附件字段名（存 subReference）
     */
    private final String attachmentField;

    /**
     * batch-update 是否执行触发器（联调建议 false）
     */
    private final boolean doTrigger;

    public OaRegReportProperties(String formCode,
                                 String rightId,
                                 String formMainTable,
                                 String formSubTable,
                                 String attachmentField,
                                 Boolean doTrigger) {
        this.formCode = formCode != null ? formCode.trim() : "";
        this.rightId = rightId != null ? rightId.trim() : "";
        this.formMainTable = blankToDefault(formMainTable, "formmain_4070");
        this.formSubTable = blankToDefault(formSubTable, "formson_5464");
        this.attachmentField = blankToDefault(attachmentField, "field0218");
        this.doTrigger = doTrigger != null && doTrigger;
    }

    public String getFormCode() {
        return formCode;
    }

    public String getRightId() {
        return rightId;
    }

    public String getFormMainTable() {
        return formMainTable;
    }

    public String getFormSubTable() {
        return formSubTable;
    }

    public String getAttachmentField() {
        return attachmentField;
    }

    public boolean isDoTrigger() {
        return doTrigger;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
