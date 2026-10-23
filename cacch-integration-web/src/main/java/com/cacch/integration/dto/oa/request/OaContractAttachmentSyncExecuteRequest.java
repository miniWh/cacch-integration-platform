package com.cacch.integration.dto.oa.request;

import lombok.Data;

/**
 * OA 货物购销合同附件回传 CRM 执行请求
 *
 * <p>由 ESB 定时调用时通常不传参数；人工补偿时可按需指定。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaContractAttachmentSyncExecuteRequest {

    /**
     * 是否全量扫描
     *
     * <p>不传或 false：先查中间表已入库 ID，按 NOT IN 分片增量扫描；
     * true：忽略排除列表做一次性全量初始化，仅人工使用。</p>
     */
    private Boolean fullScan;

    /**
     * NC 销售订单号
     *
     * <p>非空时仅处理该订单对应记录，用于快速补偿。</p>
     */
    private String ncOrderNo;
}
