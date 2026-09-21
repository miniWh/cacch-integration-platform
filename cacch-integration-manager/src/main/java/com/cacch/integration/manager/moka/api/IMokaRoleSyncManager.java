package com.cacch.integration.manager.moka.api;

/**
 * Moka 自定义角色同步编排接口 —— 从 Moka 开放平台拉取全量角色，落库本地中间表
 *
 * <p>典型调用链：Controller → syncFromMoka() → MokaRoleClient.listRoles()
 * （HTTP 调用，Manager 层不包事务）→ 字段映射 → IMokaRoleService.batchUpsert()
 * （DB 批量 upsert，Service 内部显式 {@code @Transactional}）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaRoleSyncManager {

    /**
     * 从 Moka 开放平台拉取全量自定义角色，upsert 到本地中间表
     *
     * <p>执行流程：
     * <ol>
     *     <li>调用 {@link com.cacch.integration.integration.moka.client.MokaRoleClient#listRoles()}
     *         拉取 Moka 侧全量自定义角色</li>
     *     <li>校验响应 code（0 / 200 为成功）</li>
     *     <li>DTO → DO 字段映射（roleId / roleName / description）</li>
     *     <li>调用 {@link com.cacch.integration.service.moka.api.IMokaRoleService#batchUpsert}
     *         以 role_id 为业务主键批量 upsert 落库</li>
     * </ol>
     *
     * <p>增量策略：当前每次全量拉取，以 role_id upsert 保证幂等。
     * 如需识别 Moka 侧已被删除的角色，后续可引入增量对比逻辑。</p>
     *
     * @return 同步结果（拉取数 / upsert 成功数 / 跳过数）
     */
    MokaRoleSyncResult syncFromMoka();

    /**
     * 同步执行结果
     *
     * @param totalFetched Moka 侧返回的角色总数
     * @param upserted     本地 DB 成功 upsert 的角色数
     * @param skipped      因缺失 roleId 等校验失败而跳过的条数
     */
    record MokaRoleSyncResult(int totalFetched, int upserted, int skipped) {
    }
}
