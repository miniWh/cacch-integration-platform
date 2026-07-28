package com.cacch.integration.common.config.oa;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 国内登记报告（formmain_4070 / formson_5464）CAP4 联调配置
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
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

    /**
     * 登记负责人字段名
     */
    private final String fieldOwner;

    /**
     * IPDP 名称字段名
     */
    private final String fieldIpdpName;

    /**
     * 资料项目字段名
     */
    private final String fieldItemName;

    /**
     * 子表外键列名
     */
    private final String subTableFk;

    /**
     * OA 人员表物理名（field0223 存人员 ID，需 JOIN 取 name）
     */
    private final String orgMemberTable;

    public OaRegReportProperties(String formCode,
                                 String rightId,
                                 String formMainTable,
                                 String formSubTable,
                                 String attachmentField,
                                 Boolean doTrigger,
                                 String fieldOwner,
                                 String fieldIpdpName,
                                 String fieldItemName,
                                 String subTableFk,
                                 String orgMemberTable) {
        this.formCode = formCode != null ? formCode.trim() : "";
        this.rightId = rightId != null ? rightId.trim() : "";
        this.formMainTable = blankToDefault(formMainTable, "formmain_4070");
        this.formSubTable = blankToDefault(formSubTable, "formson_5464");
        this.attachmentField = blankToDefault(attachmentField, "field0218");
        this.doTrigger = doTrigger != null && doTrigger;
        this.fieldOwner = blankToDefault(fieldOwner, "field0223");
        this.fieldIpdpName = blankToDefault(fieldIpdpName, "field0160");
        this.fieldItemName = blankToDefault(fieldItemName, "field0214");
        this.subTableFk = blankToDefault(subTableFk, "formmain_id");
        this.orgMemberTable = blankToDefault(orgMemberTable, "org_member");
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
