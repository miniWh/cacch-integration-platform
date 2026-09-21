package com.cacch.integration.service.moka.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.entity.moka.MokaRoleDO;
import com.cacch.integration.mapper.moka.MokaRoleMapper;
import com.cacch.integration.service.moka.api.IMokaRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Moka 自定义角色持久化服务实现
 *
 * <p>以 role_id 为业务主键，upsert 走 PostgreSQL
 * {@code INSERT ... ON CONFLICT (role_id) DO UPDATE} 原子语义。</p>
 *
 * <p>主键生成策略：手写 {@code @Update} upsert 不会触发 MyBatis-Plus
 * 的 {@code ASSIGN_ID}，因此在 Service 层调用 {@link IdWorker#getId()}
 * 预生成雪花 id，ON CONFLICT DO UPDATE 不覆盖 id，保证幂等。</p>
 *
 * <p>事务策略：所有 DB 写操作显式声明
 * {@code @Transactional(rollbackFor=Exception.class, propagation=REQUIRED, ...)}，
 * 单条 timeout 10s、批量 timeout 60s（数量预估 ≤ 50）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaRoleServiceImpl implements IMokaRoleService {

    private static final String BIZ = "MokaRole";

    private final MokaRoleMapper roleMapper;

    @Override
    public MokaRoleDO getByRoleId(Integer roleId) {
        return roleMapper.selectOne(new LambdaQueryWrapper<MokaRoleDO>()
                .eq(MokaRoleDO::getRoleId, roleId)
                .last("LIMIT 1"));
    }

    @Override
    public MokaRoleDO getByRoleName(String roleName) {
        return roleMapper.selectOne(new LambdaQueryWrapper<MokaRoleDO>()
                .eq(MokaRoleDO::getRoleName, roleName)
                .last("LIMIT 1"));
    }

    @Override
    public List<MokaRoleDO> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<MokaRoleDO>()
                .orderByAsc(MokaRoleDO::getRoleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public MokaRoleDO upsert(MokaRoleDO role) {
        // 预生成雪花主键 —— 手写 @Update SQL 不触发 ASSIGN_ID
        long id = role.getId() != null ? role.getId() : IdWorker.getId();
        int rows = roleMapper.upsert(id, role.getRoleId(), role.getRoleName(), role.getRole(), role.getDescription());
        if (rows == 0) {
            log.warn("【{}】upsert 影响 0 行, roleId={}", BIZ, role.getRoleId());
        }
        return getByRoleId(role.getRoleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 60)
    public int batchUpsert(List<MokaRoleDO> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            log.info("【{}】批量 upsert 空列表, 跳过", BIZ);
            return 0;
        }
        int success = 0;
        int failed = 0;
        for (MokaRoleDO role : roles) {
            try {
                // 预生成雪花主键 —— 手写 @Update SQL 不触发 ASSIGN_ID
                long id = role.getId() != null ? role.getId() : IdWorker.getId();
                roleMapper.upsert(id, role.getRoleId(), role.getRoleName(), role.getRole(), role.getDescription());
                success++;
            } catch (Exception e) {
                failed++;
                log.error("【{}】批量 upsert 单条失败, roleId={}, roleName={}, 已失败={}",
                        BIZ, role.getRoleId(), role.getRoleName(), failed, e);
            }
        }
        log.info("【{}】批量 upsert 完成, 总计={}, 成功={}, 失败={}",
                BIZ, roles.size(), success, failed);
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public int deleteAll() {
        int rows = roleMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>());
        log.info("【{}】deleteAll 完成, 删除行数={}", BIZ, rows);
        return rows;
    }
}
