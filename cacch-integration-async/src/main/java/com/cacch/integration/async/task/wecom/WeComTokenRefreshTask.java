package com.cacch.integration.async.task.wecom;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.config.wecom.WeComAppConfig;
import com.cacch.integration.common.config.wecom.WeComProperties;
import com.cacch.integration.manager.wecom.api.IWeComTokenManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 企业微信 access_token 定时刷新任务（支持多公司 + 多应用）
 *
 * <pre>
 * 启动时：@PostConstruct 遍历所有 app 配置，逐一切始化 token
 * 运行中：每 100 分钟遍历所有 app 逐一刷新（企微 token 有效期 120 分钟）
 * </pre>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComTokenRefreshTask {

    private final IWeComTokenManager weComTokenManager;
    private final WeComProperties weComProperties;

    /** Token 刷新间隔：100 分钟（毫秒） */
    private static final long REFRESH_INTERVAL_MS = 100L * 60 * 1000;

    /**
     * 应用启动时遍历所有 app 并初始化 token
     */
    @PostConstruct
    public void init() {
        log.info("【WeComRefresh】应用启动，开始初始化所有应用 access_token");
        doRefresh();
    }

    /**
     * 定时刷新：每 100 分钟执行一次
     */
    @Scheduled(initialDelay = REFRESH_INTERVAL_MS, fixedDelay = REFRESH_INTERVAL_MS)
    public void scheduledRefresh() {
        log.info("【WeComRefresh】定时任务触发，开始刷新所有应用 access_token");
        doRefresh();
    }

    /**
     * 遍历所有应用配置，逐个刷新 token
     */
    private void doRefresh() {
        List<WeComAppConfig> allApps = weComProperties.getAllApps();

        if (allApps.isEmpty()) {
            log.warn("【WeComRefresh】未配置任何企业微信应用，跳过刷新");
            return;
        }

        log.info("【WeComRefresh】共发现 {} 个应用配置，开始逐一切始化", allApps.size());

        int successCount = 0;
        int failCount = 0;

        for (WeComAppConfig app : allApps) {
            String corpid = app.getCorpid();
            String appKey = app.getAppKey();
            try {
                String token = weComTokenManager.getAccessToken(corpid, appKey);
                log.info("【WeComRefresh】刷新成功, corpid={}, appKey={}, token 前 8 位={}",
                        corpid, appKey, maskToken(token));
                successCount++;
            } catch (BizException e) {
                log.error("【WeComRefresh】刷新失败, corpid={}, appKey={}, errCode={}, errMsg={}",
                        corpid, appKey, e.getCode(), e.getMessage());
                failCount++;
            } catch (Exception e) {
                log.error("【WeComRefresh】刷新发生未知异常, corpid={}, appKey={}", corpid, appKey, e);
                failCount++;
            }
        }

        log.info("【WeComRefresh】刷新完成, 总数={}, 成功={}, 失败={}",
                allApps.size(), successCount, failCount);
    }

    /**
     * Token 脱敏：仅展示前 8 位
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 8) + "****";
    }
}
