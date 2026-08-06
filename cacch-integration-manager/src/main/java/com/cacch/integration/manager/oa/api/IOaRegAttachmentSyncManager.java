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

    /**
     * 按产品名称（OA field0160 / IPDP 名称）触发单项目或多项目附件同步
     *
     * @param productName 产品名称，须与 OA 主表 field0160 精确一致（首尾空白忽略）
     * @return 本轮统计；匹配多个主表时对每个主表各执行一轮并汇总计数
     * @throws com.cacch.integration.common.exception.BizException 产品名称无对应主表时抛出
     */
    OaRegAttachmentSyncResult syncAttachmentsByProductName(String productName);
}
