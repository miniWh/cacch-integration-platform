package com.cacch.integration.service.oa.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.integration.entity.oa.OaRegAttachmentSyncDO;

/**
 * 国内登记报告附件同步记录服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaRegAttachmentSyncService {

    /**
     * 按业务幂等键查询同步记录
     *
     * @param ownerName   登记负责人
     * @param ipdpName    IPDP 名称
     * @param itemName    资料项目名称
     * @param fileVersion 文件版本号；null 时不查询
     * @return 记录；不存在时返回 null
     */
    OaRegAttachmentSyncDO findByBizKey(String ownerName,
                                       String ipdpName,
                                       String itemName,
                                       Integer fileVersion);

    /**
     * 判断是否可跳过（已成功且 checksum 一致）
     *
     * @param existing 已有记录
     * @param checksum 当前文件 checksum
     * @return true 表示无需再次上传
     */
    boolean shouldSkipSuccess(OaRegAttachmentSyncDO existing, String checksum);

    /**
     * 写入同步成功记录
     *
     * @param record 同步记录（含业务键与 OA 回写信息）
     */
    void markSuccess(OaRegAttachmentSyncDO record);

    /**
     * 写入同步失败并递增重试次数
     *
     * @param record   待更新记录（至少含业务键）
     * @param errorMsg 失败原因
     * @param maxRetry 最大重试次数
     * @return 更新后状态码 RETRY 或 FAILED
     */
    String markFailure(OaRegAttachmentSyncDO record, String errorMsg, int maxRetry);

    /**
     * 写入跳过记录
     *
     * @param record  同步记录
     * @param message 跳过原因
     */
    void markSkipped(OaRegAttachmentSyncDO record, String message);

    /**
     * 按主表 ID 分页查询同步记录
     *
     * @param formMainId OA 主表 ID
     * @param page       页码，从 1 开始
     * @param size       每页条数
     * @return 分页结果
     */
    IPage<OaRegAttachmentSyncDO> pageByFormMainId(Long formMainId, long page, long size);

    /**
     * 分页查询同步记录（可选过滤）
     *
     * @param syncStatus 同步状态，可空
     * @param page       页码，从 1 开始
     * @param size       每页条数
     * @return 分页结果
     */
    IPage<OaRegAttachmentSyncDO> pageQuery(String syncStatus, long page, long size);
}
