package com.cacch.integration.service.moka.api;

import com.cacch.integration.entity.moka.MokaPersonDO;

import java.time.LocalDateTime;
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

    /**
     * 按 moka_sync_status 集合查询在职且未删除的人员 —— 接口 2 推送前过滤待同步记录
     *
     * <p>过滤条件：
     * <ul>
     *     <li>{@code moka_sync_status IN (...)} —— 同步状态匹配</li>
     *     <li>{@code deactivated = 0} —— 仅在职人员（离职不同步到 Moka）</li>
     *     <li>{@code is_deleted = 0} —— 未逻辑删除（@TableLogic 自动追加）</li>
     * </ul>
     * 典型调用：传入 {@code [0, 2]}（PENDING + SYNC_FAILED）做重试推送，
     * 不传 {@code 1}（SYNCED）避免重复推送已成功记录。</p>
     *
     * @param syncStatuses 同步状态集合；null 或空时返回空列表
     * @return 匹配的在职未删除人员列表（按 user_id 升序）；无数据时返回空列表
     */
    List<MokaPersonDO> listBySyncStatusIn(List<Integer> syncStatuses);

    /**
     * 批量更新同步状态 —— 接口 2 推送后回写
     *
     * <p>逐条 try-catch，单条失败不阻断其余；用于 Moka API 整批推送结果回写：
     * <ul>
     *     <li>整批成功 → status=1（SYNCED），result="success"</li>
     *     <li>整批失败 → status=2（SYNC_FAILED），result="code={xxx},msg={yyy}"</li>
     * </ul>
     * 同一批的所有记录共享相同的 status 与 result。</p>
     *
     * @param ids        待更新的主键列表；null 或空时直接返回 0
     * @param syncStatus 同步状态：1=SYNCED, 2=SYNC_FAILED
     * @param syncTime   推送时间
     * @param syncResult 推送结果摘要（允许为空）
     * @return 成功更新的条数
     */
    int batchUpdateSyncStatus(List<Long> ids, Integer syncStatus,
                              LocalDateTime syncTime, String syncResult);
}
