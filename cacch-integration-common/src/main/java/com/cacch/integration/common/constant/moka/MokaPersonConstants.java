package com.cacch.integration.common.constant.moka;

import java.util.Set;

/**
 * Moka 人员同步业务常量 —— 同步状态、自动推送门禁
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MokaPersonConstants {

    private MokaPersonConstants() {
    }

    /**
     * 日志域
     */
    public static final String LOG_BIZ = "Moka人员";

    /**
     * 0-PENDING
     */
    public static final int SYNC_PENDING = 0;
    /**
     * 1-SYNCED
     */
    public static final int SYNC_SYNCED = 1;
    /**
     * 2-SYNC_FAILED
     */
    public static final int SYNC_FAILED = 2;
    /**
     * 4-SKIP：Moka 已存在且内建角色非自动同步范围
     */
    public static final int SYNC_SKIP = 4;

    /**
     * Moka 存在性：未知
     */
    public static final int MOKA_EXISTS_UNKNOWN = 0;
    /**
     * Moka 存在性：平台已存在
     */
    public static final int MOKA_EXISTS_YES = 1;

    /**
     * 允许中台自动 push 的内建角色：内推人 / 前台 / 面试官
     */
    public static final Set<Integer> AUTO_SYNC_BUILTIN_ROLES = Set.of(0, 5, 10);

    /**
     * V3 人员列表每页条数（文档最大 500）
     */
    public static final int USER_LIST_V3_PAGE_LIMIT = 500;

    /**
     * 是否允许中台自动推送到 Moka
     *
     * @param mokaUserId      对账得到的 Moka 用户 id，null 表示尚未在平台创建
     * @param mokaBuiltinRole 对账得到的内建角色，null 表示未知（未对账或首次创建前）
     * @return true 表示可自动 push
     */
    public static boolean isAutoSyncEligible(Long mokaUserId, Integer mokaBuiltinRole) {
        if (mokaUserId == null) {
            return true;
        }
        if (mokaBuiltinRole == null) {
            return true;
        }
        return AUTO_SYNC_BUILTIN_ROLES.contains(mokaBuiltinRole);
    }
}