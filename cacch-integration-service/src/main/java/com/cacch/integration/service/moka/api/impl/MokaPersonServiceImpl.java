package com.cacch.integration.service.moka.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.entity.moka.MokaPersonDO;
import com.cacch.integration.mapper.moka.MokaPersonMapper;
import com.cacch.integration.service.moka.api.IMokaPersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

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

    /**
     * Moka 默认角色 ID —— 阶段一所有新同步人员固定使用此值
     */
    private static final int DEFAULT_ROLE_ID = 223379;

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
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public MokaPersonDO upsert(MokaPersonDO person) {
        // 预生成雪花主键 —— 手写 @Update SQL 不触发 ASSIGN_ID
        long id = person.getId() != null ? person.getId() : IdWorker.getId();
        // roleId 兜底：未显式赋值时使用 Moka 默认角色
        Integer roleId = person.getRoleId() != null ? person.getRoleId() : DEFAULT_ROLE_ID;
        int rows = personMapper.upsert(id, person.getUserId(), person.getEmployeeNo(),
                person.getUserName(), person.getNickname(), person.getCompanyEmail(),
                person.getContactPhone(), roleId, person.getDepartmentCode(),
                person.getSuperiorEmail(), person.getEmployeeStatus(),
                person.getDeactivated() != null ? person.getDeactivated() : 0);
        if (rows == 0) {
            log.warn("【{}】upsert 影响 0 行, userId={}", BIZ, person.getUserId());
        }
        return getByUserId(person.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 120)
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
                Integer roleId = person.getRoleId() != null ? person.getRoleId() : DEFAULT_ROLE_ID;
                personMapper.upsert(id, person.getUserId(), person.getEmployeeNo(),
                        person.getUserName(), person.getNickname(), person.getCompanyEmail(),
                        person.getContactPhone(), roleId, person.getDepartmentCode(),
                        person.getSuperiorEmail(), person.getEmployeeStatus(),
                        person.getDeactivated() != null ? person.getDeactivated() : 0);
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
}
