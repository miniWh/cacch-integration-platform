package com.cacch.integration.service.moka.api;

import com.cacch.integration.entity.moka.MokaPersonDO;

import java.util.List;

/**
 * Moka 人员信息持久化服务接口
 *
 * <p>职责边界：单聚合内的 DB 读写，负责按业务主键 user_id upsert、
 * 查询、清理等；不直接调用任何第三方 HTTP Client。</p>
 *
 * <p>主键生成策略：手写 {@code @Update} upsert 不会触发
 * MyBatis-Plus 的 {@code ASSIGN_ID}，由本 Service 层调用
 * {@code IdWorker.getId()} 预生成雪花 id。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaPersonService {

    /**
     * 按 user_id 查询人员
     *
     * @param userId iHR 员工 ID（persondetail.userId）
     * @return 人员实体；不存在时返回 null
     */
    MokaPersonDO getByUserId(String userId);

    /**
     * 查询全部未逻辑删除的人员
     *
     * @return 人员列表；无数据时返回空列表（非 null）
     */
    List<MokaPersonDO> listAll();

    /**
     * 单条 upsert —— 以 user_id 为业务主键，存在则覆盖字段
     *
     * <p>主键 id 在此方法内调用 {@code IdWorker.getId()} 预生成
     * （若 DO 已带 id 则复用）。</p>
     *
     * @param person 待写入的人员实体（user_id 必填）
     * @return upsert 后从 DB 重新查询的实体
     */
    MokaPersonDO upsert(MokaPersonDO person);

    /**
     * 批量 upsert —— 逐条 try-catch，单条失败不阻断其余
     *
     * @param persons 待写入的人员实体列表；null 或空时直接返回 0
     * @return 成功 upsert 的条数
     */
    int batchUpsert(List<MokaPersonDO> persons);
}
