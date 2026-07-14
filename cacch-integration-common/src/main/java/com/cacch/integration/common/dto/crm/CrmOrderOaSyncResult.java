package com.cacch.integration.common.dto.crm;

import lombok.Builder;
import lombok.Data;

/**
 * CRM 订单 OA 同步执行结果统计
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class CrmOrderOaSyncResult {

    /**
     * 本轮扫描到的待同步明细数
     */
    private int scanned;

    /**
     * 发起 OA 成功数
     */
    private int success;

    /**
     * 失败后记为 RETRY 的数量
     */
    private int retry;

    /**
     * 达到上限后记为 FAILED 的数量
     */
    private int failed;

    /**
     * 跳过数（主表缺失 / 明细未就绪等）
     */
    private int skipped;
}
