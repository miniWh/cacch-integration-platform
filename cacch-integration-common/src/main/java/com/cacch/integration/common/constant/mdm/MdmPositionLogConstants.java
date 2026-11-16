package com.cacch.integration.common.constant.mdm;

/**
 * 岗位主数据（MDM）岗位子域 —— 日志域常量
 *
 * <p>仅承载岗位子域日志业务标识；buildAction 等跨子域共享的日志工具方法
 * 见 {@link MdmLogConstants}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmPositionLogConstants {

    private MdmPositionLogConstants() {
    }

    /**
     * MDM 岗位子域日志业务标识（所有 Service / Manager / Controller 类共用，
     * 日志输出统一使用中文方括号【岗位主数据】前缀）
     */
    public static final String LOG_BIZ = "岗位主数据";
}
