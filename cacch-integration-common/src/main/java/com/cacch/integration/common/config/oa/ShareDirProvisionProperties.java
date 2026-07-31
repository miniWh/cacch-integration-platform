package com.cacch.integration.common.config.oa;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 共享盘目录治理配置（REQ-OA-002）
 *
 * <p>绑定配置前缀 {@code oa.share-dir-provision}，控制定时任务开关、批处理参数、白名单等。
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "oa.share-dir-provision")
public class ShareDirProvisionProperties {

    /**
     * 是否启用定时治理任务（默认关闭，test 验证后开启）
     */
    private final boolean enabled;

    /**
     * 定时任务 cron 表达式（建议早于附件同步任务）
     */
    private final String cron;

    /**
     * 主表（项目）每批拉取数量，对齐 OaRegReportDbClient 主表游标分批
     */
    private final int formBatchSize;

    /**
     * 仅指定 formMainId 触发时限制子表行数
     */
    private final int subRowBatchSize;

    /**
     * 测试白名单（登记负责人姓名），空表示全量
     */
    private final Set<String> ownerAllowlist;

    /**
     * SKIPPED_NOT_EMPTY 是否企微提醒（建议默认 true）
     */
    private final boolean alertNotEmptySkipped;

    /**
     * 空目录判定时忽略的文件名（默认空=不忽略，更安全）
     */
    private final Set<String> ignoreSystemFiles;

    public ShareDirProvisionProperties(Boolean enabled,
                                       String cron,
                                       Integer formBatchSize,
                                       Integer subRowBatchSize,
                                       List<String> ownerAllowlist,
                                       Boolean alertNotEmptySkipped,
                                       List<String> ignoreSystemFiles) {
        this.enabled = enabled != null && enabled;
        this.cron = blankToDefault(cron, "0 0 3 * * ?");
        this.formBatchSize = formBatchSize != null && formBatchSize > 0 ? formBatchSize : 50;
        this.subRowBatchSize = subRowBatchSize != null && subRowBatchSize > 0 ? subRowBatchSize : 500;
        this.ownerAllowlist = ownerAllowlist == null || ownerAllowlist.isEmpty()
                ? Set.of()
                : ownerAllowlist.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
        this.alertNotEmptySkipped = alertNotEmptySkipped == null || alertNotEmptySkipped;
        this.ignoreSystemFiles = ignoreSystemFiles == null || ignoreSystemFiles.isEmpty()
                ? Set.of()
                : ignoreSystemFiles.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
