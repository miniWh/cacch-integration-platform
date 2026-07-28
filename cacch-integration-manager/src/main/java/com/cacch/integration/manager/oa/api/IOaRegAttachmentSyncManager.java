package com.cacch.integration.manager.oa.api;

import com.cacch.integration.common.dto.oa.OaRegAttachmentSyncResult;

/**
 * 国内登记报告附件同步编排
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaRegAttachmentSyncManager {

    /**
     * 执行一轮附件同步（扫描 OA 资料列表行）
     *
     * @param formMainId 可选主表 ID 过滤；null 表示按 batch-size 扫描
     * @return 本轮统计
     */
    OaRegAttachmentSyncResult syncAttachments(Long formMainId);
}
