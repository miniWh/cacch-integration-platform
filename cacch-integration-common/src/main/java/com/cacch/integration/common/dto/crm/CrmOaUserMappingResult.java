package com.cacch.integration.common.dto.crm;

import lombok.Builder;
import lombok.Data;

/**
 * CRM↔OA 人员映射解析结果
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class CrmOaUserMappingResult {

    /**
     * 是否映射成功（拿到有效 oaUserId）
     */
    private boolean success;

    /**
     * 是否命中库表缓存（未走 CRM/OA 远程解析）
     */
    private boolean fromCache;

    /**
     * CRM 员工 ID（入参）
     */
    private String crmEmployeeId;

    /**
     * CRM emp_code
     */
    private String empCode;

    /**
     * OA 人员 ID
     */
    private String oaUserId;

    /**
     * OA 登录名
     */
    private String oaLoginName;

    /**
     * CRM 员工姓名
     */
    private String crmEmployeeName;

    /**
     * 失败原因（success=false 时）
     */
    private String errorMessage;

    /**
     * 构造成功结果
     *
     * @param fromCache 是否来自缓存
     * @return 结果
     */
    public static CrmOaUserMappingResult ok(String crmEmployeeId, String empCode, String oaUserId,
                                            String oaLoginName, String crmEmployeeName,
                                            boolean fromCache) {
        return CrmOaUserMappingResult.builder()
                .success(true)
                .fromCache(fromCache)
                .crmEmployeeId(crmEmployeeId)
                .empCode(empCode)
                .oaUserId(oaUserId)
                .oaLoginName(oaLoginName)
                .crmEmployeeName(crmEmployeeName)
                .build();
    }

    /**
     * 构造失败结果
     *
     * @param crmEmployeeId CRM 员工 ID
     * @param errorMessage  失败原因
     * @return 结果
     */
    public static CrmOaUserMappingResult fail(String crmEmployeeId, String errorMessage) {
        return CrmOaUserMappingResult.builder()
                .success(false)
                .fromCache(false)
                .crmEmployeeId(crmEmployeeId)
                .errorMessage(errorMessage)
                .build();
    }
}
