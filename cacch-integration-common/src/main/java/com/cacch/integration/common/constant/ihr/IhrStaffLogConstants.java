package com.cacch.integration.common.constant.ihr;

/**
 * IHR 员工域日志标识常量类（仅用于日志域，不含业务参数）
 *
 * <p>所有员工域 Client / Service 的日志业务标识 BIZ 统一从本类取，
 * 各接口 ACTION 描述通过 {@link #buildAction(String)} 构造，
 * 避免业务类中硬编码字符串。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class IhrStaffLogConstants {

    private IhrStaffLogConstants() {
    }

    /**
     * 员工域统一日志业务标识 — 员工域所有 Client / Service 类共用
     *
     * <p>对应 logback 输出中的中文方括号业务标识 {@code 【iHR员工域】}。</p>
     */
    public static final String LOG_BIZ = "iHR员工域";

    /**
     * 构造单接口操作描述（统一格式，供 Client 层 ACTION 常量复用）
     *
     * <p>当前直接返回入参字符串；后续若需加前缀或格式化可在此统一调整，
     * 业务类无感知。</p>
     *
     * @param action 操作名称（如 "获取员工ID清单"）
     * @return 格式化后的操作标识
     */
    public static String buildAction(String action) {
        return action;
    }
}
