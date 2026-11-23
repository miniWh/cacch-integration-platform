package com.cacch.integration.manager.mdm.emp.incremental.support;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 人员同步执行互斥（全量 / 增量 Pull / 增量 Process 独立锁）
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
public class MdmEmpSyncExecutionGuard {

    private final AtomicBoolean fullSyncRunning = new AtomicBoolean();
    private final AtomicBoolean pullIncrementalRunning = new AtomicBoolean();
    private final AtomicBoolean processIncrementalRunning = new AtomicBoolean();

    /**
     * 在全量互斥下执行
     *
     * @param action 全量同步体
     * @param <T>    返回类型
     * @return action 返回值
     * @throws BizException 上一轮全量仍在执行
     */
    public <T> T executeFullSync(Supplier<T> action) {
        if (!fullSyncRunning.compareAndSet(false, true)) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "上一轮全量同步仍在执行中，请勿重复触发");
        }
        try {
            return action.get();
        } finally {
            fullSyncRunning.set(false);
        }
    }

    /**
     * 尝试获取 Pull 增量锁
     *
     * @return true 表示已获取
     */
    public boolean tryAcquirePull() {
        return pullIncrementalRunning.compareAndSet(false, true);
    }

    /**
     * 释放 Pull 增量锁
     */
    public void releasePull() {
        pullIncrementalRunning.set(false);
    }

    /**
     * 尝试获取 Process 增量锁
     *
     * @return true 表示已获取
     */
    public boolean tryAcquireProcess() {
        return processIncrementalRunning.compareAndSet(false, true);
    }

    /**
     * 释放 Process 增量锁
     */
    public void releaseProcess() {
        processIncrementalRunning.set(false);
    }
}