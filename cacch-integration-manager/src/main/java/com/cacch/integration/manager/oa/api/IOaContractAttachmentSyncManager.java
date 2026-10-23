package com.cacch.integration.manager.oa.api;

import com.cacch.integration.common.dto.oa.OaContractAttachmentSyncResult;

/**
 * OA 货物购销合同附件回传 CRM 编排
 *
 * <p>由 ESB 定时调用中台执行接口触发，本层负责：执行锁、OA 增量扫描入库、
 * 待处理数据抢占、线程池并发回传 CRM 与告警。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaContractAttachmentSyncManager {

    /**
     * 执行一次扫描与回传
     *
     * @param fullScan  true 表示忽略中间表排除列表做一次性全量初始化；false 表示按 NOT IN 增量扫描
     * @param ncOrderNo 可选 NC 销售订单号；非空时仅处理该订单对应记录，用于快速补偿
     * @return 本次执行统计；未获取到执行锁或功能停用时 executed=false 并携带原因
     */
    OaContractAttachmentSyncResult execute(boolean fullScan, String ncOrderNo);
}
