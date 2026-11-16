package com.cacch.integration.common.constant.mdm;

/**
 * 部门主数据（MDM）部门子域 —— 日志域常量
 *
 * <p>仅承载部门子域日志业务标识；buildAction 等跨子域共享的日志工具方法
 * 见 {@link MdmLogConstants}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmDeptLogConstants {

    private MdmDeptLogConstants() {
    }

    /**
     * MDM 部门子域日志业务标识（所有 Service / Manager / Controller 类共用，
     * 日志输出统一使用中文方括号【部门主数据】前缀）
     */
    public static final String LOG_BIZ = "部门主数据";
}
