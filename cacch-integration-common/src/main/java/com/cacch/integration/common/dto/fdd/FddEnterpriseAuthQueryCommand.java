package com.cacch.integration.common.dto.fdd;

/**
 * 企业实名认证查询/发起命令
 *
 * <p>查询与发起均须传联系人 personName + mobile，按「内部企业+姓名+手机号」校验个人实名；
 * 发起认证时还可传 idNumber（须与已实名联系人一致），用于法大大 createCompany 绑定。</p>
 *
 * @param internalCompanyName 内部企业全称（必填，业务判定键之一）
 * @param enterpriseName      外部企业名称（发起认证时必填）
 * @param uscc                统一社会信用代码（必填，业务判定键之一）
 * @param personName          企业联系人/管理员姓名（必填）
 * @param idNumber            企业联系人身份证号（发起时可传，须与已实名记录一致）
 * @param mobile              企业联系人手机号（必填，与姓名联合定位联系人）
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
