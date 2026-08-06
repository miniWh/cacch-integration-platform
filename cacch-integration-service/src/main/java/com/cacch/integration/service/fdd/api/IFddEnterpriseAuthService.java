package com.cacch.integration.service.fdd.api;

import com.cacch.integration.entity.fdd.FddEnterpriseAuthDO;

/**
 * 法大大企业实名认证服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IFddEnterpriseAuthService {

    /**
     * 按内部企业 + uscc 查询 SUCCESS 记录（全局最多一条）
     *
     * @param internalCompanyName 内部企业全称
     * @param uscc                统一社会信用代码
     * @return SUCCESS 记录；不存在时返回 null
     */
    FddEnterpriseAuthDO findSuccess(String internalCompanyName, String uscc);

    /**
     * 按内部企业 + uscc 查询最新一条 PENDING 记录
     *
     * @param internalCompanyName 内部企业全称
     * @param uscc                统一社会信用代码
     * @return 最新 PENDING；不存在时返回 null
     */
    FddEnterpriseAuthDO findLatestPending(String internalCompanyName, String uscc);

    /**
     * 按内部企业 + uscc 查询最新一条 FAILED 记录
     *
     * @param internalCompanyName 内部企业全称
     * @param uscc                统一社会信用代码
     * @return 最新 FAILED；不存在时返回 null
     */
    FddEnterpriseAuthDO findLatestFailed(String internalCompanyName, String uscc);

    /**
     * 是否存在任一 FAILED 历史记录（用于判定 isRepeatVerified）
     *
     * @param internalCompanyName 内部企业全称
     * @param uscc                统一社会信用代码
     * @return true 表示曾失败过
     */
    boolean hasFailedHistory(String internalCompanyName, String uscc);

    /**
     * 新增 PENDING 认证记录
     *
     * @param record 待插入记录
     * @return 插入后的记录（含主键）
     */
    FddEnterpriseAuthDO insertPending(FddEnterpriseAuthDO record);

    /**
     * 按 transactionNo 查询记录（回调匹配）
     *
     * @param transactionNo 法大大流水号
     * @return 认证记录；不存在时返回 null
     */
    FddEnterpriseAuthDO findByTransactionNo(String transactionNo);

    /**
     * 回调更新认证结果
     *
     * @param id           记录主键
     * @param authStatus   SUCCESS / FAILED
     * @param authDetail   回调原始报文
     * @param failReason   失败原因（失败时）
     * @param fddCompanyId 法大大 companyId（可空）
     * @param fddAccountId 法大大管理员 accountId（可空）
     */
    void updateByCallback(Long id, String authStatus, Object authDetail, String failReason,
                          String fddCompanyId, String fddAccountId);

    /**
     * 法大大侧已实名时落库 SUCCESS（查询同步）
     *
     * @param record 认证通过记录
     * @return 插入后的记录
     */
    FddEnterpriseAuthDO insertSuccessFromRemote(FddEnterpriseAuthDO record);

    /**
     * 将历史终态记录重置为 PENDING，供测试环境回放企业回调（正式认证流程禁止调用）
     *
     * @param id 企业认证记录主键
     * @return 重置后的记录
     */
    FddEnterpriseAuthDO resetToPendingForCallbackTest(Long id);
}
