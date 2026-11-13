package com.cacch.integration.service.mdm.api;

import com.cacch.integration.common.enums.mdm.MdmDeptIngestResultEnum;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;

/**
 * 部门主数据（MDM）单部门落库 Service 接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMdmDeptIngestService {

    /**
     * 单部门落库（单部门事务粒度，纯 DB 操作）
     *
     * <p>幂等规则：
     * <ul>
     *     <li>rowHash 未变化：跳过后续所有处理（仅写 stg）</li>
     *     <li>事件幂等键 = SHA-256(deptUuid:eventType:rowHash)，重复事件吞掉</li>
     *     <li>parentDeptCode 直接存 iHR parentDepartmentCode，不做主键自引用，
     *         父部门晚于子部门到达（乱序建档）不阻断落库</li>
     * </ul></p>
     *
     * @param dept       iHR 部门实体（非 null，须包含 uuid）
     * @param rawJson    iHR 原始响应报文原文（贴源用，可为 null）
     * @param etlBatchNo ETL 批次号（本轮拉取唯一）
     * @return 落库结果（已落库 / 无变化跳过）
     * @throws com.cacch.integration.common.exception.BizException DB 写入失败或事件 payload 序列化失败时抛出（事务回滚）
     */
    MdmDeptIngestResultEnum ingestOne(IhrDepartment dept, String rawJson, String etlBatchNo);
}
