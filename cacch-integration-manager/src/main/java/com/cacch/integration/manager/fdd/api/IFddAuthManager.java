package com.cacch.integration.manager.fdd.api;

import com.cacch.integration.common.dto.fdd.FddAuthQueryResult;
import com.cacch.integration.common.dto.fdd.FddEnterpriseAuthQueryCommand;
import com.cacch.integration.common.dto.fdd.FddPersonAuthQueryCommand;
import com.cacch.integration.integration.fdd.client.dto.FddCallbackRequest;

/**
 * 法大大认证编排接口（企业 / 个人）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IFddAuthManager {

    /**
     * 企业实名认证查询 / 自动发起
     *
     * <p>按 internalCompanyName + uscc 判定；已 SUCCESS 直接返回；无 SUCCESS 且无 PENDING 时可自动发起。</p>
     *
     * @param command 查询/发起命令
     * @return 认证查询结果
     */
    FddAuthQueryResult queryOrAuthEnterprise(FddEnterpriseAuthQueryCommand command);

    /**
     * 个人实名认证查询 / 自动发起
     *
     * <p>按 internalCompanyName + idNumber + mobile 判定；已 SUCCESS 直接返回；无 SUCCESS 且无 PENDING 时可自动发起。换手机号视为未认证。</p>
     *
     * @param command 查询/发起命令
     * @return 认证查询结果
     */
    FddAuthQueryResult queryOrAuthPerson(FddPersonAuthQueryCommand command);

    /**
     * 处理法大大企业认证回调
     *
     * @param request    回调请求体
     * @param rawPayload 原始回调报文（用于入库）
     */
    void handleEnterpriseCallback(FddCallbackRequest request, Object rawPayload);

    /**
     * 处理法大大个人认证回调
     *
     * @param request    回调请求体
     * @param rawPayload 原始回调报文（用于入库）
     */
    void handlePersonCallback(FddCallbackRequest request, Object rawPayload);
}
