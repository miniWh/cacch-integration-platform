package com.cacch.integration.service.keruyun.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.common.constant.keruyun.KeruyunOrderSyncStatus;
import com.cacch.integration.entity.keruyun.KeruyunOrderSyncTaskDO;
import com.cacch.integration.mapper.keruyun.KeruyunOrderSyncTaskMapper;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderSyncTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 客如云订单同步任务控制表服务实现
 *
 * <p>提供任务创建、状态机 CAS 流转、断点续传进度更新与终态标记。
 * 创建与状态流转使用数据库唯一键 + 乐观锁保证并发安全。</p>
 *
 * <p><b>事务设计要点</b>：{@link #createTask} 使用 {@link TransactionTemplate}
 * 以 {@link Propagation#REQUIRES_NEW} 编程式事务执行，确保每次任务创建在独立事务中，
 * 唯一键冲突只回滚自己的小事务，不影响外层循环（见 {@link #createMissingTasks}）。
 * 不使用 Spring 声明式事务的原因是外层循环为同类自调用，声明式 REQUIRES_NEW 不生效。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeruyunOrderSyncTaskServiceImpl implements IKeruyunOrderSyncTaskService {

    private static final String BIZ = "客如云订单同步任务";

    /**
     * 当前需要同步的 dateType 集合
     *
     * <p>后续要扩展其他类型（FINISH_BUSI_DATE / FINISH_TIME / SETTLE_TIME）时，
     * 只需在此 Set 中追加即可，无需改动其他代码。</p>
     */
    private static final Set<String> SYNC_DATE_TYPES = Set.of("OPEN_TIME");

    private final KeruyunOrderSyncTaskMapper taskMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    public KeruyunOrderSyncTaskDO getById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public List<KeruyunOrderSyncTaskDO> findAllPending() {
        LambdaQueryWrapper<KeruyunOrderSyncTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KeruyunOrderSyncTaskDO::getStatus, KeruyunOrderSyncStatus.PENDING.name())
                .orderByAsc(KeruyunOrderSyncTaskDO::getSyncDate);
        return taskMapper.selectList(wrapper);
    }

    @Override
    public List<KeruyunOrderSyncTaskDO> findAllRetryable() {
        LambdaQueryWrapper<KeruyunOrderSyncTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KeruyunOrderSyncTaskDO::getStatus,
                        KeruyunOrderSyncStatus.FAILED.name(), KeruyunOrderSyncStatus.PARTIAL.name())
                .orderByAsc(KeruyunOrderSyncTaskDO::getSyncDate);
        return taskMapper.selectList(wrapper);
    }

    @Override
    public KeruyunOrderSyncTaskDO createTask(LocalDate syncDate, String dateType) {
        // 编程式 REQUIRES_NEW：每次创建独立事务，唯一键冲突只回滚自己的小事务
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        transactionTemplate.setTimeout(10);
        return transactionTemplate.execute(status -> {
            LocalDateTime start = syncDate.atStartOfDay();
            LocalDateTime end = syncDate.atTime(LocalTime.MAX).withNano(0);

            KeruyunOrderSyncTaskDO task = new KeruyunOrderSyncTaskDO();
            task.setSyncDate(syncDate);
            task.setDateType(dateType);
            task.setSyncWindowStart(start);
            task.setSyncWindowEnd(end);
            task.setStatus(KeruyunOrderSyncStatus.PENDING.name());
            task.setRetryCount(0);
            task.setLastPage(0);
            task.setSuccessCount(0);
            task.setFailureCount(0);
            task.setVersion(0);

            try {
                taskMapper.insert(task);
                log.info("【{}】创建任务成功, syncDate={}, dateType={}, id={}",
                        BIZ, syncDate, dateType, task.getId());
                return task;
            } catch (DuplicateKeyException e) {
                log.info("【{}】任务已存在跳过, syncDate={}, dateType={}", BIZ, syncDate, dateType);
                status.setRollbackOnly();
                return null;
            }
        });
    }

    @Override
    public int createMissingTasks(LocalDate from, LocalDate to) {
        int created = 0;
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            dates.add(d);
        }

        for (LocalDate date : dates) {
            for (String dateType : SYNC_DATE_TYPES) {
                KeruyunOrderSyncTaskDO t = createTask(date, dateType);
                if (t != null) {
                    created++;
                }
            }
        }
        log.info("【{}】批量创建任务完成, from={}, to={}, created={}", BIZ, from, to, created);
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public boolean transitionStatus(Long id, String expectedStatus, String newStatus) {
        KeruyunOrderSyncTaskDO db = taskMapper.selectById(id);
        if (db == null) {
            log.info("【{}】状态流转终止, reason=任务不存在, id={}", BIZ, id);
            return false;
        }
        // 如果是 IN_PROGRESS 且存在 lastPage，允许断点续传（不要求状态精确匹配）
        if (KeruyunOrderSyncStatus.IN_PROGRESS.name().equals(db.getStatus())
                && KeruyunOrderSyncStatus.IN_PROGRESS.name().equals(expectedStatus)
                && db.getLastPage() != null && db.getLastPage() > 0) {
            log.info("【{}】断点续传, id={}, lastPage={}", BIZ, id, db.getLastPage());
            return true;
        }
        int affected = taskMapper.updateStatusWithCas(id, expectedStatus, newStatus, db.getVersion());
        if (affected == 0) {
            log.info("【{}】状态流转终止, reason=CAS失败, id={}, expected={}, actual={}",
                    BIZ, id, expectedStatus, db.getStatus());
        }
        return affected > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public void updateProgress(Long id, int lastPage, Integer totalPage, int successCount, int failureCount) {
        KeruyunOrderSyncTaskDO db = taskMapper.selectById(id);
        if (db == null) {
            log.info("【{}】进度更新终止, reason=任务不存在, id={}", BIZ, id);
            return;
        }
        db.setLastPage(lastPage);
        if (totalPage != null) {
            db.setTotalPage(totalPage);
        }
        db.setSuccessCount(successCount);
        db.setFailureCount(failureCount);
        db.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public void markComplete(Long id, String finalStatus, String errorMessage) {
        KeruyunOrderSyncTaskDO db = taskMapper.selectById(id);
        if (db == null) {
            log.info("【{}】标记完成终止, reason=任务不存在, id={}", BIZ, id);
            return;
        }
        db.setStatus(finalStatus);
        db.setSyncedAt(LocalDateTime.now());
        if (StringUtils.hasText(errorMessage)) {
            db.setErrorMessage(truncate(errorMessage, 500));
        }
        db.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(db);
        log.info("【{}】标记终态, id={}, finalStatus={}, errorLen={}",
                BIZ, id, finalStatus, errorMessage == null ? 0 : errorMessage.length());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public boolean retry(Long id) {
        KeruyunOrderSyncTaskDO db = taskMapper.selectById(id);
        if (db == null) {
            log.info("【{}】重试终止, reason=任务不存在, id={}", BIZ, id);
            return false;
        }
        if (KeruyunOrderSyncStatus.IN_PROGRESS.name().equals(db.getStatus())) {
            log.info("【{}】重试终止, reason=任务正在执行中, id={}", BIZ, id);
            return false;
        }
        db.setStatus(KeruyunOrderSyncStatus.PENDING.name());
        db.setRetryCount((db.getRetryCount() == null ? 0 : db.getRetryCount()) + 1);
        db.setLastPage(0);
        db.setSuccessCount(0);
        db.setFailureCount(0);
        db.setErrorMessage(null);
        db.setTotalPage(null);
        db.setTotalCount(null);
        db.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(db);
        log.info("【{}】任务已重置为 PENDING 状态, id={}, retryCount={}", BIZ, id, db.getRetryCount());
        return true;
    }

    /**
     * 截断字符串到指定长度
     */
    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLen ? value.substring(0, maxLen) + "..." : value;
    }
}
