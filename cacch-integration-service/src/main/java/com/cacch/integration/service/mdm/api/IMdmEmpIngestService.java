package com.cacch.integration.service.mdm.api;

import com.cacch.integration.common.enums.mdm.MdmEmpIngestResultEnum;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicInfo;

/**
 * 人员主数据（MDM）单员工落库 Service 接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMdmEmpIngestService {

    /**
     * 单员工落库（单员工事务粒度，纯 DB 操作）
     *
     * <p>处理顺序：stg 贴源（幂等）→ rowHash 比对幂等短路 → ods upsert →
     * 证件号质检 → mdm_emp 主档 upsert（按 idHash 识别自然人）→
     * 任职拉链判定（HIRE/REHIRE/LEAVE/TRANSFER/UPDATE）→ mdm_emp_event 幂等插入。</p>
     *
     * <p>幂等与质检规则：
     * <ul>
     *     <li>rowHash 未变化：跳过后续所有处理（仅写 stg）</li>
     *     <li>证件号缺失：致命项，stg/ods 落库后终止，不产主档与事件</li>
     *     <li>离职无离职日期：置 dq_flag=1 后继续</li>
     *     <li>事件幂等键 = SHA-256(mdCode:eventType:rowHash)，重复事件吞掉</li>
     * </ul></p>
     *
     * @param basic      iHR 员工基本信息（非 null）
     * @param rawJson    iHR 原始响应报文原文（贴源用，可为 null）
     * @param etlBatchNo ETL 批次号（本轮拉取唯一）
     * @return 落库结果（已落库 / 无变化跳过 / 证件号缺失跳过）
     * @throws com.cacch.integration.common.exception.BizException DB 写入失败等不可恢复错误时抛出（事务回滚）
     */
    MdmEmpIngestResultEnum ingestOne(IhrStaffBasicInfo basic, String rawJson, String etlBatchNo);
}
