package com.cacch.integration.config.keruyun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 客如云订单同步异步线程池配置
 *
 * <p>专门用于跑单任务的异步执行，避免接口超时。
 * 与通用线程池隔离，防止被大量同步任务阻塞。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableAsync
public class KeruyunAsyncConfig {

    /**
     * 客如云订单同步异步线程池
     *
     * <p>核心线程 2、最大 4、队列 50，足够支撑按天粒度的批量同步。
     * 队列满时抛出 RejectedExecutionException，不做静默丢弃——
     * Controller 层捕获后直接标记任务为 FAILED 并附带拒绝原因。</p>
     */
    @Bean("keruyunOrderSyncExecutor")
    public Executor keruyunOrderSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("keruyun-order-sync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
