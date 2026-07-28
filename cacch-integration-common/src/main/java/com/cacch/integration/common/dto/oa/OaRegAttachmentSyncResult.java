package com.cacch.integration.common.dto.oa;

import lombok.Builder;
import lombok.Data;

/**
 * 国内登记报告附件同步执行结果统计
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class OaRegAttachmentSyncResult {

    /**
     * 本轮扫描到的资料行数
     */
    private int scanned;

    /**
     * 同步成功数
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
     * 跳过数（目录缺失 / 无文件 / 幂等等）
     */
    private int skipped;
}
