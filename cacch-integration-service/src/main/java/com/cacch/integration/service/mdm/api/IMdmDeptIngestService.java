package com.cacch.integration.service.mdm.api;

import com.cacch.integration.common.enums.mdm.MdmDeptIngestResultEnum;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;

import java.util.Collection;
import java.util.List;

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
     *     <li>rowHash 未变化：跳过后续所有处理（不写 stg 贴源层）</li>
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

    /**
     * 停用比对 —— 将 mdm_dept 中已不在 iHR 存活集合内的部门置为停用并写 DISABLE 事件
     *
     * <p>逐条处理 mdm_dept 未逻辑删除行：{@code deptUuid} 不在存活集且当前状态非 DISABLE 的，
     * 置 {@code dept_status='DISABLE'} 并写 DISABLE/HIGH 事件（eventId =
     * SHA-256(deptUuid:DISABLE:etlBatchNo)，同批次幂等）；已是停用状态的跳过。
     * 存活集为空时直接返回 0（防调用方异常场景误停用全量）。</p>
     *
     * @param aliveDeptUuids 本轮 iHR 全量拉取到的部门 uuid 集合（非空才执行比对）
     * @param etlBatchNo     ETL 批次号（事件幂等键组成部分）
     * @return 本轮实际停用的部门数量
     * @throws com.cacch.integration.common.exception.BizException DB 写入失败或事件 payload 序列化失败时抛出（事务回滚）
     */
    int disableMissing(Collection<String> aliveDeptUuids, String etlBatchNo);

    /**
     * 按 iHR 部门 ID 解析部门编码（ods_ihr_dept.ihr_dept_id → department_code）
     *
     * <p>供员工链路将 iHR departmentId 映射为任职组织编码；
     * 映射缺失（部门主数据未同步或 iHR 新增滞后）返回 null，由调用方决定回退策略。</p>
     *
     * @param ihrDeptId iHR 部门 ID（数字字符串形式；null 或空时直接返回 null）
     * @return 部门编码；未匹配时返回 null
     */
    String resolveDeptCodeByIhrDeptId(String ihrDeptId);

    /**
     * 部门主数据一致性核对（mdm_dept 与 ods_ihr_dept 双向 uuid 差集）
     *
     * <p>全量同步完成后调用：比对两层的未逻辑删除记录数量与 uuid 集合，
     * 差集非空说明存在落库中断或数据不一致，需要人工介入。</p>
     *
     * @return 核对结果（两侧数量 + 双向缺失 uuid 列表）
     */
    DeptVerifyResult verifyWithOds();

    /**
     * 一致性核对结果
     *
     * @param mdmCount     mdm_dept 未逻辑删除记录数
     * @param odsCount     ods_ihr_dept 未逻辑删除记录数
     * @param missingInMdm ods 存在但 mdm 缺失的 uuid 列表（落库中断嫌疑）
     * @param missingInOds mdm 存在但 ods 缺失的 uuid 列表（数据异常嫌疑）
     */
    record DeptVerifyResult(
            int mdmCount,
            int odsCount,
            List<String> missingInMdm,
            List<String> missingInOds
    ) {
    }
}
