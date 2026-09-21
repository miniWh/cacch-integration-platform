package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.entity.moka.MokaRoleDO;
import com.cacch.integration.integration.moka.client.MokaRoleClient;
import com.cacch.integration.integration.moka.client.dto.MokaRoleItem;
import com.cacch.integration.integration.moka.client.dto.MokaRoleListResponse;
import com.cacch.integration.manager.moka.api.IMokaRoleSyncManager;
import com.cacch.integration.service.moka.api.IMokaRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Moka 自定义角色同步编排实现 —— 从 Moka 开放平台拉取全量角色，upsert 到本地 PG 中间表
 *
 * <p>调用链：Controller → syncFromMoka() → MokaRoleClient.listRoles()
 * （HTTP 调用）→ DTO 转 DO → IMokaRoleService.batchUpsert()（DB 写）。</p>
 *
 * <p>事务策略：Manager 层不在 HTTP 调用或整个编排外层包裹
 * {@code @Transactional}，避免 HTTP 耗时持有 DB 连接。
 * 批量落库的事务由 {@link IMokaRoleService#batchUpsert} 内部显式声明。</p>
 *
 * <p>字段映射（MokaRoleItem → MokaRoleDO）：
 * <ul>
 *     <li>id → roleId（业务主键，必填）</li>
 *     <li>name → roleName（角色名称）</li>
 *     <li>role → role（整数角色值）</li>
 *     <li>description → description（可选描述）</li>
 * </ul>
 * </p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaRoleSyncManagerImpl implements IMokaRoleSyncManager {

    private static final String BIZ = "Moka 角色信息同步";

    private final MokaRoleClient mokaRoleClient;
    private final IMokaRoleService mokaRoleService;

    @Override
    public MokaRoleSyncResult syncFromMoka() {
        log.info("【{}】开始从 Moka 开放平台拉取全量自定义角色", BIZ);

        // —— 1. HTTP 调用拉取 Moka 侧角色 ——
        MokaRoleListResponse response;
        try {
            response = mokaRoleClient.listRoles();
        } catch (Exception e) {
            // Client 内部已打印日志，这里补一层业务兜底
            log.info("【{}】拉取 Moka 角色失败, 跳过本次同步, reason={}", BIZ, e.getMessage());
            return new MokaRoleSyncResult(0, 0, 0);
        }

        if (response.getData() == null || response.getData().isEmpty()) {
            log.info("【{}】Moka 侧无自定义角色（或响应 data 为空）, 同步结束", BIZ);
            return new MokaRoleSyncResult(0, 0, 0);
        }

        List<MokaRoleItem> items = response.getData();
        int totalFetched = items.size();
        log.info("【{}】Moka 侧返回角色总数={}", BIZ, totalFetched);

        // —— 2. 字段映射 + 校验 ——
        List<MokaRoleDO> batch = new ArrayList<>(totalFetched);
        int skipped = 0;
        for (MokaRoleItem item : items) {
            // 校验：id 为空则跳过（无业务主键无法 upsert）
            if (item.getId() == null) {
                skipped++;
                log.info("【{}】跳过无 id 的角色项, name={}", BIZ, item.getName());
                continue;
            }

            MokaRoleDO roleDO = new MokaRoleDO();
            roleDO.setRoleId(item.getId());
            roleDO.setRoleName(item.getName());
            roleDO.setRole(item.getRole());
            roleDO.setDescription(item.getDescription());
            batch.add(roleDO);
        }

        if (batch.isEmpty()) {
            log.info("【{}】全部角色项无有效 id, 同步结束, totalFetched={}, skipped={}",
                    BIZ, totalFetched, skipped);
            return new MokaRoleSyncResult(totalFetched, 0, skipped);
        }

        // —— 3. 批量 upsert 落库 ——
        log.info("【{}】开始批量 upsert 落库, totalFetched={}, validToUpsert={}, skipped={}",
                BIZ, totalFetched, batch.size(), skipped);

        int upserted = mokaRoleService.batchUpsert(batch);

        log.info("【{}】同步完成, totalFetched={}, upserted={}, skipped={}",
                BIZ, totalFetched, upserted, skipped);

        return new MokaRoleSyncResult(totalFetched, upserted, skipped);
    }
}
