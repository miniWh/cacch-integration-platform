package com.cacch.integration.manager.crm.api;

import com.cacch.integration.common.dto.crm.CrmOaUserMappingResult;
import com.cacch.integration.entity.crm.CrmOaUserMappingDO;

/**
 * CRM↔OA 人员映射编排（独立能力）
 *
 * <p>链路：crmEmployeeId →（库表缓存）→ CRM queryEmployee(emp_code)
 * → OA orgMembers/code → UPSERT 落库 → 返回 oaUserId。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmOaUserMappingManager {

    /**
     * 解析 OA 人员 ID（优先读库；未命中或无效则远程解析并落库）
     *
     * @param crmEmployeeId CRM 员工 ID（订单 owner.id），不可为空
     * @return 映射结果；失败时 {@code success=false} 并带 errorMessage（不抛业务异常，便于同步记 RETRY）
     */
    CrmOaUserMappingResult resolve(String crmEmployeeId);

    /**
     * 解析 OA 人员 ID
     *
     * @param crmEmployeeId CRM 员工 ID，不可为空
     * @param forceRefresh  true 时忽略库表有效缓存，强制走 CRM/OA 重新解析并更新
     * @return 映射结果
     */
    CrmOaUserMappingResult resolve(String crmEmployeeId, boolean forceRefresh);

    /**
     * 仅查询库表映射（不调三方）
     *
     * @param crmEmployeeId CRM 员工 ID
     * @return 映射记录；不存在时返回 null
     */
    CrmOaUserMappingDO getCached(String crmEmployeeId);
}
