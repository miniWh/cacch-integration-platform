package com.cacch.integration.service.oa.api;

import com.cacch.integration.integration.oa.client.dto.OaContractRow;

import java.util.List;

/**
 * OA 货物购销合同附件回传 CRM 中间记录服务
 *
 * <p>负责中间记录表的读写：扫描入库、待处理捞取、状态回写与同步状态查询。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaContractAttachmentSyncService {

    /**
     * 查询中间表已入库的全部 OA 购销合同主表 ID（NOT IN 增量扫描排除列表数据源）
     *
     * @return 已入库 oa_form_main_id 列表；无数据时返回空列表（非 null）
     */
    List<Long> listExistingFormMainIds();

    /**
     * 扫描结果幂等写入中间表
     *
     * <p>按 oa_form_main_id 去重后批量 UPSERT：不存在则新增（PENDING），已存在仅刷新
     * OA 快照字段，不覆盖同步状态。</p>
     *
     * @param rows OA 购销合同扫描行，可空
     * @return 本次写入的记录数（去重后）
     */
    int upsertFromScan(List<OaContractRow> rows);
}
