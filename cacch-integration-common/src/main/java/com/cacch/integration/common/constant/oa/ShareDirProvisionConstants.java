package com.cacch.integration.common.constant.oa;

import com.cacch.integration.common.constant.redis.RedisConstants;

/**
 * 共享盘目录治理常量（REQ-OA-002）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class ShareDirProvisionConstants {

    public static final String LOG_BIZ = "OaShareDirProvision";

    // ── Redis 游标 Key（与 REQ-OA-001 附件同步隔离） ──

    /**
     * 主表分批游标 Redis Key（值为已处理批次中最大 formmain_4070.id）
     */
    public static final String PROVISION_CURSOR_REDIS_KEY =
            RedisConstants.KEY_PREFIX + "oa:reg-report:provision:cursor";

    /**
     * 分布式锁前缀（按 L3 sharePath 加锁）
     */
    public static final String PROVISION_LOCK_PREFIX =
            RedisConstants.KEY_PREFIX + "oa:share-provision:lock:";

    // ── Action 枚举值 ──

    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_DELETED = "DELETED";
    public static final String ACTION_SKIPPED_EXISTS = "SKIPPED_EXISTS";
    public static final String ACTION_SKIPPED_NOT_EMPTY = "SKIPPED_NOT_EMPTY";
    public static final String ACTION_SKIPPED_NOT_REQUIRED = "SKIPPED_NOT_REQUIRED";
    public static final String ACTION_SKIPPED_GROUP_RETAINED = "SKIPPED_GROUP_RETAINED";
    public static final String ACTION_FAILED = "FAILED";

    // ── field0216 存储值 ──

    /**
     * field0216 = "1" 表示不需要
     */
    public static final String ITEM_REQUIRED_NO = "1";

    private ShareDirProvisionConstants() {
    }
}
