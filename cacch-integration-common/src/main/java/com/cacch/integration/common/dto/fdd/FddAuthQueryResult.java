package com.cacch.integration.common.dto.fdd;

import java.time.LocalDateTime;

/**
 * 法大大认证查询/发起结果
 *
 * @param certified           是否已认证通过
 * @param needAuth            是否已发起或存在进行中认证
 * @param status              认证状态 PENDING / SUCCESS / FAILED
 * @param authType            认证类型 ENTERPRISE / PERSON
 * @param internalCompanyName 内部企业全称
 * @param authUrl             法大大认证页面 URL
 * @param enterpriseName      外部企业名称
 * @param uscc                统一社会信用代码
 * @param personName          个人姓名
 * @param idNumber            身份证号
 * @param mobile              手机号
 * @param sourceSystem        当前命中记录的发起来源（审计回显）
 * @param failReason          失败原因
 * @param certifiedAt         认证通过时间
 * @param canRetry            是否可重试（FAILED 时为 true）
 * @param message             提示信息
 * @author hongfu_zhou@cacch.com
 */
public record FddAuthQueryResult(
        boolean certified,
        boolean needAuth,
        String status,
        String authType,
        String internalCompanyName,
        String authUrl,
        String enterpriseName,
        String uscc,
        String personName,
        String idNumber,
        String mobile,
        String sourceSystem,
        String failReason,
        LocalDateTime certifiedAt,
        Boolean canRetry,
        String message
) {
}
