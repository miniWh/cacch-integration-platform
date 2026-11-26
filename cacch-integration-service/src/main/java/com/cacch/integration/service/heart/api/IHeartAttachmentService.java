package com.cacch.integration.service.heart.api;

/**
 * Heart 客户 PO 附件上传服务
 *
 * <p>当前附件为固定本地文件（{@code HeartAttachmentConstants.DEFAULT_ATTACHMENT_FILE_PATH}），
 * 后续由邮件解析结果替换；上传至 NAS 挂载目录后回写主表 {@code attachment_url}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IHeartAttachmentService {

    /**
     * 上传 PO 附件并回写可访问 URL
     *
     * <p>幂等策略：{@code attachment_url} 已有值时直接复用，不重复落盘。</p>
     *
     * @param poId PO 主键
     * @throws com.cacch.integration.common.exception.BizException poId 为空、PO 不存在、
     *                                                             本地文件不可读或落盘失败、URL 回写失败时抛出
     */
    void resolveAndSaveAttachment(Long poId);
}
