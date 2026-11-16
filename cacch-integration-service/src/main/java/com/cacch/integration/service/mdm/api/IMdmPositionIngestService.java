package com.cacch.integration.service.mdm.api;

import com.cacch.integration.common.enums.mdm.MdmIngestResultEnum;
import com.cacch.integration.integration.ihr.client.dto.IhrPosition;

import java.util.Collection;
import java.util.List;

/**
 * 岗位主数据（MDM）单岗位落库 Service 接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMdmPositionIngestService {

    /**
     * 单岗位落库（单岗位事务粒度，纯 DB 操作）
     *
     * <p>幂等规则：
     * <ul>
     *     <li>rowHash 未变化：跳过后续所有处理（不写 stg 贴源层）</li>
     *     <li>事件幂等键 = SHA-256(positionUuid:eventType:rowHash)，重复事件吞掉</li>
     *     <li>parentPositionId 直接存 iHR parentId（UUID），不做主键自引用，
     *         父岗位晚于子岗位到达（乱序建档）不阻断落库</li>
     * </ul></p>
     *
     * @param position   iHR 职位实体（非 null，须包含 id 即职位 uuid）
     * @param rawJson    iHR 原始响应报文原文（贴源用，可为 null）
     * @param etlBatchNo ETL 批次号（本轮拉取唯一）
     * @return 落库结果（已落库 / 无变化跳过）
     * @throws com.cacch.integration.common.exception.BizException DB 写入失败或事件 payload 序列化失败时抛出（事务回滚）
     */
    MdmIngestResultEnum ingestOne(IhrPosition position, String rawJson, String etlBatchNo);

    /**
     * 停用比对 —— 将 mdm_position 中已不在 iHR 存活集合内的岗位置为停用并写 DISABLE 事件
     *
     * <p>逐条处理 mdm_position 未逻辑删除行：经 src_ref（IHR / POSITION）反查各主档行的
     * iHR uuid 后与存活集比对；无 iHR 映射的岗位不受本轮停用比对管辖（多源保护）；
     * uuid 不在存活集且状态非 DISABLE 的置 {@code position_state='DISABLE'} 并写事件
     * （eventId = SHA-256(positionUuid:DISABLE:etlBatchNo)，同批次幂等）；
     * 已停用的跳过。存活集为空时直接返回 0（防调用方异常场景误停用全量）。</p>
     *
     * @param alivePositionUuids 本轮 iHR 全量拉取到的岗位 uuid 集合（非空才执行比对）
     * @param etlBatchNo         ETL 批次号（事件幂等键组成部分）
     * @return 本轮实际停用的岗位数量
     * @throws com.cacch.integration.common.exception.BizException DB 写入失败或事件 payload 序列化失败时抛出（事务回滚）
     */
    int disableMissing(Collection<String> alivePositionUuids, String etlBatchNo);

    /**
     * 岗位主数据一致性核对（mdm_position 与 ods_ihr_position 双向 uuid 差集）
     *
     * <p>全量同步完成后调用：比对两层的未逻辑删除记录数量与 uuid 集合，
     * 差集非空说明存在落库中断或数据不一致，需要人工介入。</p>
     *
     * @return 核对结果（两侧数量 + 双向缺失 uuid 列表）
     */
    PositionVerifyResult verifyWithOds();

    /**
     * 一致性核对结果
     *
     * @param mdmCount     mdm_position 未逻辑删除记录数
     * @param odsCount     ods_ihr_position 未逻辑删除记录数
     * @param missingInMdm ods 存在但 mdm 缺失的 uuid 列表（落库中断嫌疑）
     * @param missingInOds mdm 存在但 ods 缺失的 uuid 列表（数据异常嫌疑）
     */
    record PositionVerifyResult(
            int mdmCount,
            int odsCount,
            List<String> missingInMdm,
            List<String> missingInOds
    ) {
    }
}
