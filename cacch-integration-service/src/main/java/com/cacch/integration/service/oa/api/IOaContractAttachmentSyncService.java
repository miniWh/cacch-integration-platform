package com.cacch.integration.service.oa.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.integration.entity.oa.OaContractAttachmentSyncDO;

import java.time.LocalDateTime;

/**
 * OA 货物购销合同附件回传 CRM 中间记录服务
 *
 * <p>负责中间记录表的读写：扫描入库、待处理捞取、状态回写与同步状态查询。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaContractAttachmentSyncService {

    /**
     * 分页查询同步状态记录
     *
     * @param syncStatus 同步状态，可空；取值见 {@code OaContractAttachmentSyncStatusEnum}
     * @param ncOrderNo  NC 销售订单号，可空；非空时精确匹配
     * @param startTime  OA 流程发起时间下限，可空
     * @param endTime    OA 流程发起时间上限，可空
     * @param page       页码，从 1 开始
     * @param size       每页条数，最大 100
     * @return 分页结果，按 updatedAt、id 倒序
     */
    IPage<OaContractAttachmentSyncDO> pageQuery(String syncStatus,
                                                String ncOrderNo,
                                                LocalDateTime startTime,
                                                LocalDateTime endTime,
                                                long page,
                                                long size);

    /**
     * 将指定记录重置为待处理，供人工补偿
     *
     * @param id 中间记录主键
     * @return true 表示重置成功；记录不存在或已是 SUCCESS 时返回 false
     */
    boolean resetRetry(Long id);

    /**
     * 按 NC 销售订单号批量重置为待处理
     *
     * @param ncOrderNo NC 销售订单号，非空
     * @return 实际重置的记录数
     */
    int resetRetryByNcOrderNo(String ncOrderNo);
}
