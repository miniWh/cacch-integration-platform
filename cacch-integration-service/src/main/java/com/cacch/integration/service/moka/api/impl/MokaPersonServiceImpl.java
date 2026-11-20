package com.cacch.integration.service.moka.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.common.constant.moka.MokaPersonConstants;
import com.cacch.integration.entity.moka.MokaPersonDO;
import com.cacch.integration.mapper.moka.MokaPersonMapper;
import com.cacch.integration.service.moka.api.IMokaPersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Moka 人员信息持久化服务实现
 *
 * <p>以 user_id 为业务主键，upsert 走 PostgreSQL
 * {@code INSERT ... ON CONFLICT (user_id) DO UPDATE} 原子语义。
 * 主键 id 在 INSERT 时由 Service 层预生成，ON CONFLICT DO UPDATE
 * 不覆盖 id，保证幂等。</p>
 *
 * <p>事务策略：所有 DB 写操作显式声明
 * {@code @Transactional(rollbackFor=Exception.class, propagation=REQUIRED, ...)}，
 * 单条 timeout 10s、批量 timeout 120s（人员量预估 ≤ 1000）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaPersonServiceImpl implements IMokaPersonService {

    private static final String BIZ = "MokaPerson";

    private final MokaPersonMapper personMapper;

    @Override
    public MokaPersonDO getByUserId(String userId) {
        return personMapper.selectOne(new LambdaQueryWrapper<MokaPersonDO>()
                .eq(MokaPersonDO::getUserId, userId)
                .last("LIMIT 1"));
    }

    @Override
    public List<MokaPersonDO> listAll() {
        return personMapper.selectList(new LambdaQueryWrapper<MokaPersonDO>()
                .orderByAsc(MokaPersonDO::getUserId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 10)
    public MokaPersonDO upsert(MokaPersonDO person) {
        // 预生成雪花主键 —— 手写 @Update SQL 不触发 ASSIGN_ID
        long id = person.getId() != null ? person.getId() : IdWorker.getId();
        // roleId 兜底：未显式赋值时使用 Moka 默认角色
        Integer roleId = person.getRoleId() != null ? person.getRoleId() : MokaConstants.DEFAULT_ROLE_ID;
        int rows = personMapper.upsertFromIhr(id, person.getUserId(), person.getEmployeeNo(),
                person.getUserName(), person.getNickname(), person.getCompanyEmail(),
                person.getContactPhone(), roleId, person.getDepartmentCode(),
                person.getEmployeeStatus(),
                person.getDeactivated() != null ? person.getDeactivated() : 0,
                person.getMokaSyncStatus() != null ? person.getMokaSyncStatus() : MokaPersonConstants.SYNC_PENDING);
        if (rows == 0) {
            log.warn("【{}】upsert 影响 0 行, userId={}", BIZ, person.getUserId());
        }
        return getByUserId(person.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 120)
    public int batchUpsert(List<MokaPersonDO> persons) {
        if (CollectionUtils.isEmpty(persons)) {
            log.info("【{}】批量 upsert 空列表, 跳过", BIZ);
            return 0;
        }
        int success = 0;
        int failed = 0;
        for (MokaPersonDO person : persons) {
            try {
                // 预生成雪花主键 —— 手写 @Update SQL 不触发 ASSIGN_ID
                long id = person.getId() != null ? person.getId() : IdWorker.getId();
                // roleId 兜底：未显式赋值时使用 Moka 默认角色
                Integer roleId = person.getRoleId() != null ? person.getRoleId() : MokaConstants.DEFAULT_ROLE_ID;
                personMapper.upsertFromIhr(id, person.getUserId(), person.getEmployeeNo(),
                        person.getUserName(), person.getNickname(), person.getCompanyEmail(),
                        person.getContactPhone(), roleId, person.getDepartmentCode(),
                        person.getEmployeeStatus(),
                        person.getDeactivated() != null ? person.getDeactivated() : 0,
                        person.getMokaSyncStatus() != null ? person.getMokaSyncStatus() : MokaPersonConstants.SYNC_PENDING);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("【{}】批量 upsert 单条失败, userId={}, userName={}, 已失败={}",
                        BIZ, person.getUserId(), person.getUserName(), failed, e);
            }
        }
        log.info("【{}】批量 upsert 完成, 总计={}, 成功={}, 失败={}",
                BIZ, persons.size(), success, failed);
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 120)
    public int batchUpsertFromIhr(List<MokaPersonDO> persons) {
        if (CollectionUtils.isEmpty(persons)) {
            log.info("【{}】batchUpsertFromIhr 空列表, 跳过", BIZ);
            return 0;
        }
        Map<String, MokaPersonDO> existingByUserId = new HashMap<>();
        for (MokaPersonDO existing : listAll()) {
            if (existing != null && StringUtils.hasText(existing.getUserId())) {
                existingByUserId.put(existing.getUserId(), existing);
            }
        }

        int success = 0;
        for (MokaPersonDO incoming : persons) {
            if (incoming == null || !StringUtils.hasText(incoming.getUserId())) {
                continue;
            }
            try {
                MokaPersonDO existing = existingByUserId.get(incoming.getUserId());
                long id = existing != null && existing.getId() != null
                        ? existing.getId() : IdWorker.getId();
                Integer roleId = existing != null && existing.getRoleId() != null
                        ? existing.getRoleId() : MokaConstants.DEFAULT_ROLE_ID;
                Integer syncStatus = resolveSyncStatusAfterIhrChange(existing, incoming);
                personMapper.upsertFromIhr(id, incoming.getUserId(), incoming.getEmployeeNo(),
                        incoming.getUserName(), incoming.getNickname(), incoming.getCompanyEmail(),
                        incoming.getContactPhone(), roleId, incoming.getDepartmentCode(),
                        incoming.getEmployeeStatus(),
                        incoming.getDeactivated() != null ? incoming.getDeactivated() : 0,
                        syncStatus);
                success++;
            } catch (Exception e) {
                log.error("【{}】batchUpsertFromIhr 单条失败, userId={}", BIZ, incoming.getUserId(), e);
            }
        }
        log.info("【{}】batchUpsertFromIhr 完成, 总计={}, 成功={}", BIZ, persons.size(), success);
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 120)
    public int reconcilePlatformSnapshots(List<IMokaPersonService.MokaPlatformUserSnapshot> snapshots,
                                          LocalDateTime syncedAt) {
        if (CollectionUtils.isEmpty(snapshots) || syncedAt == null) {
            log.info("【{}】reconcilePlatformSnapshots 入参为空, 跳过", BIZ);
            return 0;
        }
        Map<String, IMokaPersonService.MokaPlatformUserSnapshot> byPhone = new HashMap<>();
        for (IMokaPersonService.MokaPlatformUserSnapshot snap : snapshots) {
            if (snap == null || !StringUtils.hasText(snap.phone())) {
                continue;
            }
            byPhone.put(normalizePhone(snap.phone()), snap);
        }
        if (byPhone.isEmpty()) {
            log.info("【{}】reconcilePlatformSnapshots 无有效手机号, 跳过", BIZ);
            return 0;
        }

        int updated = 0;
        List<MokaPersonDO> all = listAll();
        for (MokaPersonDO person : all) {
            if (person == null || person.getId() == null || !StringUtils.hasText(person.getContactPhone())) {
                continue;
            }
            IMokaPersonService.MokaPlatformUserSnapshot snap =
                    byPhone.get(normalizePhone(person.getContactPhone()));
            if (snap == null) {
                continue;
            }
            int nextStatus = person.getMokaSyncStatus() != null ? person.getMokaSyncStatus() : MokaPersonConstants.SYNC_PENDING;
            if (!MokaPersonConstants.isAutoSyncEligible(snap.mokaUserId(), snap.mokaBuiltinRole())
                    && (nextStatus == MokaPersonConstants.SYNC_PENDING || nextStatus == MokaPersonConstants.SYNC_SYNCED)) {
                nextStatus = MokaPersonConstants.SYNC_SKIP;
            }
            Integer roleId = snap.roleId() != null ? snap.roleId() : person.getRoleId();
            if (roleId == null) {
                roleId = MokaConstants.DEFAULT_ROLE_ID;
            }
            int rows = personMapper.updateMokaReconcileSnapshot(
                    person.getId(),
                    snap.mokaUserId(),
                    roleId,
                    snap.mokaBuiltinRole(),
                    MokaPersonConstants.MOKA_EXISTS_YES,
                    nextStatus,
                    syncedAt);
            if (rows > 0) {
                updated++;
            }
        }
        log.info("【{}】reconcilePlatformSnapshots 完成, snapshotPhones={}, localUpdated={}",
                BIZ, byPhone.size(), updated);
        return updated;
    }

    private static Integer resolveSyncStatusAfterIhrChange(MokaPersonDO existing, MokaPersonDO incoming) {
        if (existing == null) {
            return MokaPersonConstants.SYNC_PENDING;
        }
        if (!ihrBusinessFieldsChanged(existing, incoming)) {
            return null;
        }
        if (MokaPersonConstants.isAutoSyncEligible(existing.getMokaUserId(), existing.getMokaBuiltinRole())) {
            return MokaPersonConstants.SYNC_PENDING;
        }
        return MokaPersonConstants.SYNC_SKIP;
    }

    private static boolean ihrBusinessFieldsChanged(MokaPersonDO existing, MokaPersonDO incoming) {
        return !Objects.equals(existing.getEmployeeNo(), incoming.getEmployeeNo())
                || !Objects.equals(existing.getUserName(), incoming.getUserName())
                || !Objects.equals(existing.getNickname(), incoming.getNickname())
                || !Objects.equals(existing.getCompanyEmail(), incoming.getCompanyEmail())
                || !Objects.equals(existing.getContactPhone(), incoming.getContactPhone())
                || !Objects.equals(existing.getDepartmentCode(), incoming.getDepartmentCode())
                || !Objects.equals(existing.getEmployeeStatus(), incoming.getEmployeeStatus())
                || !Objects.equals(existing.getDeactivated(), incoming.getDeactivated());
    }

    private static String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }

    @Override
    public List<MokaPersonDO> listBySyncStatusIn(List<Integer> syncStatuses) {
        if (CollectionUtils.isEmpty(syncStatuses)) {
            log.info("【{}】listBySyncStatusIn 入参为空, 返回空列表", BIZ);
            return Collections.emptyList();
        }
        return personMapper.selectList(new LambdaQueryWrapper<MokaPersonDO>()
                .in(MokaPersonDO::getMokaSyncStatus, syncStatuses)
                .eq(MokaPersonDO::getDeactivated, 0)
                .orderByAsc(MokaPersonDO::getUserId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 120)
    public int batchUpdateSyncStatus(List<Long> ids, Integer syncStatus,
                                     LocalDateTime syncTime, String syncResult) {
        if (CollectionUtils.isEmpty(ids)) {
            log.info("【{}】批量更新同步状态入参 ids 为空, 跳过", BIZ);
            return 0;
        }
        int success = 0;
        int failed = 0;
        for (Long id : ids) {
            if (id == null) {
                log.info("【{}】批量更新同步状态遇 null id, 跳过", BIZ);
                continue;
            }
            try {
                int rows = personMapper.updateSyncStatus(id, syncStatus, syncTime, syncResult);
                if (rows > 0) {
                    success++;
                } else {
                    log.info("【{}】批量更新同步状态影响 0 行, id={}（可能已逻辑删除）", BIZ, id);
                }
            } catch (Exception e) {
                failed++;
                log.error("【{}】批量更新同步状态单条失败, id={}, 已失败={}",
                        BIZ, id, failed, e);
            }
        }
        log.info("【{}】批量更新同步状态完成, 总计={}, 成功={}, 失败={}, syncStatus={}",
                BIZ, ids.size(), success, failed, syncStatus);
        return success;
    }
}
