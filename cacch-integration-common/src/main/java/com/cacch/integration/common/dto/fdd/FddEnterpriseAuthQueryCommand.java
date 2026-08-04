package com.cacch.integration.common.dto.fdd;

/**
 * 企业实名认证查询/发起命令
 *
 * @param internalCompanyName 内部企业全称（必填，业务判定键之一）
 * @param enterpriseName      外部企业名称（发起认证时必填）
 * @param uscc                统一社会信用代码（必填，业务判定键之一）
 * @param autoAuth            是否自动发起认证，默认 true
 * @param sourceSystem        来源系统 CRM/OA（新发起认证时必填）
 * @param sourceBizNo         来源系统业务单号（可选）
 * @author hongfu_zhou@cacch.com
 */
public record FddEnterpriseAuthQueryCommand(
        String internalCompanyName,
        String enterpriseName,
        String uscc,
        Boolean autoAuth,
        String sourceSystem,
        String sourceBizNo
) {
}
