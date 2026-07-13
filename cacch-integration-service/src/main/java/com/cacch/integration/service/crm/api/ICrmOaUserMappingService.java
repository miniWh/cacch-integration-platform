package com.cacch.integration.service.crm.api;

import com.cacch.integration.entity.crm.CrmOaUserMappingDO;

/**
 * CRM↔OA 人员映射持久化服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmOaUserMappingService {

    /**
     * 按 CRM 员工 ID 查询映射
     *
     * @param crmEmployeeId CRM 员工 ID，不可为空
     * @return 映射记录；不存在时返回 null
     */
    CrmOaUserMappingDO getByCrmEmployeeId(String crmEmployeeId);

    /**
     * 按 CRM 员工 ID UPSERT 映射（允许更新 emp_code / oa_user_id 等）
     *
     * @param mapping 映射实体，crmEmployeeId 不可为空
     * @return 落库后的记录
     */
    CrmOaUserMappingDO saveOrUpdateByCrmEmployeeId(CrmOaUserMappingDO mapping);
}
