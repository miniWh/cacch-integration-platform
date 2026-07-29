package com.cacch.integration.manager.oa.api;

import com.cacch.integration.common.dto.oa.OaRegAttachmentSyncResult;

/**
 * 国内登记报告附件同步编排
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaRegAttachmentSyncManager {

    /**
     * 执行一轮附件同步（共享盘驱动：先扫描有文件的目录，再反查 OA 资料行）
     *
     * @param formMainId 可选主表 ID 过滤；null 表示按 owner-filter / batch-size 扫描共享盘
     * @return 本轮统计
     */
    OaRegAttachmentSyncResult syncAttachments(Long formMainId);
}
