package com.cacch.integration.service.heart.api;

/**
 * Heart 客户 PO 明细 BOM 版本解析服务
 *
 * <p>按明细商品编码前 9 位查询勤策 BOM 对象（{@code object_XbTYD__c}），
 * 取名称包含 GA/GX 的记录 ID 回写明细 {@code bom_version_id}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IHeartBomVersionService {

    /**
     * 解析并回写 PO 明细 BOM 版本
     *
     * <p>幂等策略：明细已有 {@code bom_version_id} 时跳过查询。
     * 无法确定版本时不抛异常，返回跳过原因由调用方记录备注并终止本次建单。</p>
     *
     * @param poId PO 主键
     * @return 跳过原因；全部解析成功返回 {@code null}
     * @throws com.cacch.integration.common.exception.BizException poId 为空、
     *                                                             PO 明细为空或查询调用失败时抛出
     */
    String resolveAndSaveBomVersions(Long poId);
}
