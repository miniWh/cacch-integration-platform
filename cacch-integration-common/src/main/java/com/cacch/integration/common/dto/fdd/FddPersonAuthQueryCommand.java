package com.cacch.integration.common.dto.fdd;

/**
 * 个人实名认证查询/发起命令
 *
 * @param internalCompanyName 内部企业全称（必填，业务判定键之一）
 * @param personName          姓名（发起认证时必填）
 * @param idNumber            身份证号（必填，业务判定键之一）
 * @param mobile              手机号（必填，业务判定键之一；换号视为未认证）
 * @param autoAuth            是否自动发起认证，默认 true
 * @param sourceSystem        来源系统 CRM/OA（新发起认证时必填）
 * @param sourceBizNo         来源系统业务单号（可选）
 * @author hongfu_zhou@cacch.com
 */
public record FddPersonAuthQueryCommand(
        String internalCompanyName,
        String personName,
        String idNumber,
        String mobile,
        Boolean autoAuth,
        String sourceSystem,
        String sourceBizNo
) {
}
