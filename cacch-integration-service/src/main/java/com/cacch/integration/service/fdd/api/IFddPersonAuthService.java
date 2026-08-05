package com.cacch.integration.service.fdd.api;

import com.cacch.integration.entity.fdd.FddPersonAuthDO;

/**
 * 法大大个人实名认证服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IFddPersonAuthService {

    /**
     * 按内部企业 + 身份证号 + 手机号 查询 SUCCESS 记录（组合下最多一条）
     *
     * @param internalCompanyName 内部企业全称
     * @param idNumber            身份证号
     * @param mobile              手机号
     * @return SUCCESS 记录；不存在时返回 null
     */
    FddPersonAuthDO findSuccess(String internalCompanyName, String idNumber, String mobile);

    /**
     * 按内部企业 + 身份证号 + 手机号 查询最新一条 PENDING 记录
     *
     * @param internalCompanyName 内部企业全称
     * @param idNumber            身份证号
     * @param mobile              手机号
     * @return 最新 PENDING；不存在时返回 null
     */
    FddPersonAuthDO findLatestPending(String internalCompanyName, String idNumber, String mobile);

    /**
     * 按内部企业 + 身份证号 + 手机号 查询最新一条 FAILED 记录
     *
     * @param internalCompanyName 内部企业全称
     * @param idNumber            身份证号
     * @param mobile              手机号
     * @return 最新 FAILED；不存在时返回 null
     */
    FddPersonAuthDO findLatestFailed(String internalCompanyName, String idNumber, String mobile);

    /**
     * 是否存在任一 FAILED 历史记录（用于判定 verifiedType）
     *
     * @param internalCompanyName 内部企业全称
     * @param idNumber            身份证号
     * @param mobile              手机号
     * @return true 表示曾失败过
     */
    boolean hasFailedHistory(String internalCompanyName, String idNumber, String mobile);

    /**
     * 新增 PENDING 认证记录
     *
     * @param record 待插入记录（须含 mobile）
     * @return 插入后的记录（含主键）
     */
    FddPersonAuthDO insertPending(FddPersonAuthDO record);

    /**
     * 按 transactionNo 查询记录（回调匹配）
     *
     * @param transactionNo 法大大流水号
     * @return 认证记录；不存在时返回 null
     */
    FddPersonAuthDO findByTransactionNo(String transactionNo);

    /**
     * 回调更新认证结果
     *
     * @param id           记录主键
     * @param authStatus   SUCCESS / FAILED
     * @param authDetail   回调原始报文
     * @param failReason   失败原因（失败时）
     * @param fddAccountId 法大大 accountId（可空，有则回填）
     */
    void updateByCallback(Long id, String authStatus, Object authDetail, String failReason, String fddAccountId);

    /**
     * 法大大侧已实名时落库 SUCCESS（查询同步）
     *
     * @param record 认证通过记录（须含 mobile）
     * @return 插入后的记录
     */
    FddPersonAuthDO insertSuccessFromRemote(FddPersonAuthDO record);
}
