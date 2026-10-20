package com.cacch.integration.service.moka.api.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.entity.moka.MokaPersonStatusDO;
import com.cacch.integration.mapper.moka.MokaPersonStatusMapper;
import com.cacch.integration.service.moka.api.IMokaPersonStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Moka 人员入离职状态持久化服务实现
 *
 * <p>以 employee_no（工号）为业务唯一键，状态回写走 PostgreSQL
 * {@code INSERT ... ON CONFLICT (employee_no) DO UPDATE} 原子语义。
 * 主键 id 在 INSERT 时由 Service 层预生成，ON CONFLICT DO UPDATE
 * 不覆盖 id，保证幂等可重跑。</p>
 *
 * <p>事务策略：DB 写操作显式声明
 * {@code @Transactional(rollbackFor=Exception.class, propagation=REQUIRED,
 * readOnly=false, timeout=30)}；查询不包事务。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaPersonStatusServiceImpl implements IMokaPersonStatusService {

    private static final String BIZ = "MokaPersonStatus";

    private final MokaPersonStatusMapper mokaPersonStatusMapper;

    @Override
    public List<MokaPersonStatusDO> listHiredPending() {
        return mokaPersonStatusMapper.listHiredPending();
    }

    @Override
    public List<MokaPersonStatusDO> listResignPending() {
        return mokaPersonStatusMapper.listResignPending();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public int upsertHiredResult(MokaPersonStatusDO item, Integer syncStatus,
                                 String error, LocalDateTime syncTime, Integer applicationId) {
        // 预生成雪花主键 —— 手写 @Update SQL 不触发 ASSIGN_ID
        long id = item.getId() != null ? item.getId() : IdWorker.getId();
        int rows = mokaPersonStatusMapper.upsertHiredResult(id, item.getEmployeeNo(), item.getUserId(),
                item.getUserName(), item.getContactPhone(), applicationId, item.getEmployeeStatus(),
                syncStatus, error, syncTime);
        if (rows == 0) {
            log.warn("【{}】入职同步结果回写影响 0 行, employeeNo={}", BIZ, item.getEmployeeNo());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public int upsertResignResult(MokaPersonStatusDO item, Integer syncStatus,
                                  String error, LocalDateTime syncTime, Integer applicationId) {
        // 预生成雪花主键 —— 手写 @Update SQL 不触发 ASSIGN_ID
        long id = item.getId() != null ? item.getId() : IdWorker.getId();
        int rows = mokaPersonStatusMapper.upsertResignResult(id, item.getEmployeeNo(), item.getUserId(),
                item.getUserName(), item.getContactPhone(), applicationId, item.getEmployeeStatus(),
                syncStatus, error, syncTime);
        if (rows == 0) {
            log.warn("【{}】离职同步结果回写影响 0 行, employeeNo={}", BIZ, item.getEmployeeNo());
        }
        return rows;
    }
}
