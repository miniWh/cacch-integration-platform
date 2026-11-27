package com.cacch.integration.common.dto.heart;

import com.cacch.integration.common.dto.batch.BatchProcessResult;
import lombok.Getter;
import lombok.Setter;

/**
 * Heart 邮件拉取入库执行结果
 *
 * <p>成功 / 失败 / 跳过计数与明细由 {@link BatchProcessResult} 统一维护，
 * 本类仅补充执行层面的统计字段。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@Setter
public class HeartMailPullResult extends BatchProcessResult<HeartMailPullItem> {

    /**
     * 本轮从邮箱拉取的邮件数
     */
    private int pulled;

    /**
     * 本轮重试处理的历史失败邮件数
     */
    private int retried;
}
