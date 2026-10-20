package com.cacch.integration.service.moka.api;

import com.cacch.integration.entity.moka.MokaPersonStatusDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Moka 人员入离职状态持久化服务接口
 *
 * <p>职责边界：单聚合内的 DB 读写，负责 t_integration_moka_person_status
 * 状态表的待处理集合查询与同步结果回写；不直接调用任何第三方 HTTP Client。</p>
 *
 * <p>主键生成策略：手写 {@code @Update} upsert 不会触发
 * MyBatis-Plus 的 {@code ASSIGN_ID}，由本 Service 层调用
 * {@code IdWorker.getId()} 预生成雪花 id。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaPersonStatusService {

    /**
     * 查询入职待处理集合
     *
     * <p>待处理口径：{@code t_integration_moka_person.employee_status='IN_SERVICE'}
     * 且状态行缺失或 {@code hired_sync_status IN (0, 2)}。</p>
     *
     * @return 入职待处理记录（按工号升序）；无数据时返回空列表（非 null）
     */
    List<MokaPersonStatusDO> listHiredPending();

    /**
     * 查询离职待处理集合
     *
     * <p>待处理口径：{@code t_integration_moka_person.employee_status='QUIT'}
     * 且状态行缺失或 {@code resign_sync_status IN (0, 2)}。</p>
     *
     * @return 离职待处理记录（按工号升序）；无数据时返回空列表（非 null）
     */
    List<MokaPersonStatusDO> listResignPending();

    /**
     * 回写入职同步结果 —— 以 employee_no 为业务唯一键 upsert
     *
     * <p>状态行不存在时新增，已存在时仅刷新入职同步相关字段；
     * 主键 id 在此方法内调用 {@code IdWorker.getId()} 预生成。</p>
     *
     * @param item          待处理记录（来自待处理集合查询，含工号与冗余快照字段）
     * @param syncStatus    入职同步状态：1=SYNCED, 2=SYNC_FAILED
     * @param error         失败原因摘要（允许为空）
     * @param syncTime      同步时间
     * @param applicationId Moka 申请 id（允许为空，为空时保留状态行原值）
     * @return 受影响行数；0 表示回写失败
     */
    int upsertHiredResult(MokaPersonStatusDO item, Integer syncStatus,
                          String error, LocalDateTime syncTime, Integer applicationId);

    /**
     * 回写离职同步结果 —— 以 employee_no 为业务唯一键 upsert
     *
     * <p>状态行不存在时新增，已存在时仅刷新离职同步相关字段；
     * 主键 id 在此方法内调用 {@code IdWorker.getId()} 预生成。</p>
     *
     * @param item          待处理记录（来自待处理集合查询，含工号与冗余快照字段）
     * @param syncStatus    离职同步状态：1=SYNCED, 2=SYNC_FAILED
     * @param error         失败原因摘要（允许为空）
     * @param syncTime      同步时间
     * @param applicationId Moka 申请 id（允许为空，为空时保留状态行原值）
     * @return 受影响行数；0 表示回写失败
     */
    int upsertResignResult(MokaPersonStatusDO item, Integer syncStatus,
                           String error, LocalDateTime syncTime, Integer applicationId);
}
