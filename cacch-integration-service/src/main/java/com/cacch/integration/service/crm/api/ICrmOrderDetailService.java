package com.cacch.integration.service.crm.api;

import com.cacch.integration.entity.crm.CrmOrderDetailDO;

import java.util.List;

/**
 * CRM 订单明细持久化服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmOrderDetailService {

    /**
     * 按 CRM 明细 ID 判断是否已存在
     *
     * @param crmDetailId CRM 明细 id
     * @return true 表示已存在
     */
    boolean existsByCrmDetailId(String crmDetailId);

    /**
     * 新增明细（仅新增）
     *
     * @param detail 明细实体
     * @return 插入后实体
     */
    CrmOrderDetailDO insert(CrmOrderDetailDO detail);

    /**
     * 查询待 OA 同步明细（PENDING / RETRY 且重试次数未达上限）
     *
     * @param limit    最大条数；&lt;=0 返回空列表
     * @param maxRetry 最大重试次数（不含）；retry_count &lt; maxRetry
     * @return 明细列表；无数据返回空列表
     */
    List<CrmOrderDetailDO> listPendingOrRetry(int limit, int maxRetry);

    /**
     * OA 同步成功回写
     *
     * @param detailId    明细主键
     * @param oaProcessId OA 流程实例 ID
     */
    void markOaSyncSuccess(Long detailId, String oaProcessId);

    /**
     * OA 同步 / 人员映射失败回写（递增 retry_count；达上限则 FAILED）
     *
     * @param detailId   明细主键
     * @param errorMsg   失败原因
     * @param maxRetry   最大重试次数；达到后状态为 FAILED
     * @return 更新后的状态码（RETRY 或 FAILED）
     */
    String markOaSyncFailure(Long detailId, String errorMsg, int maxRetry);
}
