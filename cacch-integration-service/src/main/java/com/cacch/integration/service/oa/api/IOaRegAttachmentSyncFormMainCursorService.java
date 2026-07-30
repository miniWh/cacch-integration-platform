package com.cacch.integration.service.oa.api;

/**
 * 国内登记报告附件同步主表分批游标服务
 *
 * <p>定时全量同步时，记录已扫描 OA 主表批次的上界 ID，下一轮从该 ID 之后继续。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaRegAttachmentSyncFormMainCursorService {

    /**
     * 读取当前游标（已处理批次中最大 formmain_4070.id 原文；空或 0 表示从头开始）
     *
     * @return 游标 ID 字符串；未设置时返回 {@code "0"}
     */
    String getLastFormMainId();

    /**
     * 保存游标（本轮批次处理完成后调用）
     *
     * @param lastFormMainId 本轮批次中最大主表 ID 原文，不可为空
     */
    void saveLastFormMainId(String lastFormMainId);

    /**
     * 重置游标为 0（从头轮询主表）
     */
    void resetCursor();
}
