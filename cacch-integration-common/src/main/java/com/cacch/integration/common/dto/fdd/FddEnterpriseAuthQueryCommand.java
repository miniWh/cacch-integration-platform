package com.cacch.integration.common.dto.fdd;

/**
 * 企业实名认证查询/发起命令
 *
 * <p>按法大大流程：发起企业认证前须先完成管理员个人实名；本命令需携带管理员三要素。</p>
 *
 * @param internalCompanyName 内部企业全称（必填，业务判定键之一）
 * @param enterpriseName      外部企业名称（发起认证时必填）
 * @param uscc                统一社会信用代码（必填，业务判定键之一）
 * @param personName          企业管理员姓名（发起认证时必填）
 * @param idNumber            企业管理员身份证号（发起认证时必填）
 * @param mobile              企业管理员手机号（发起认证时必填）
 * @param autoAuth            是否自动发起认证，默认 true
 * @param sourceSystem        来源系统 CRM/OA（新发起认证时必填）
 * @param sourceBizNo         来源系统业务单号（可选）
 * @author hongfu_zhou@cacch.com
 */
public record FddEnterpriseAuthQueryCommand(
        String internalCompanyName,
        String enterpriseName,
        String uscc,
        String personName,
        String idNumber,
        String mobile,
        Boolean autoAuth,
        String sourceSystem,
        String sourceBizNo
) {
}
