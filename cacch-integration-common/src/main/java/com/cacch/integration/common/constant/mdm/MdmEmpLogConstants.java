package com.cacch.integration.common.constant.mdm;

/**
 * 人员主数据（MDM）人员子域 —— 日志域常量
 *
 * <p>仅承载人员子域日志业务标识；buildAction 等跨子域共享的日志工具方法
 * 见 {@link MdmLogConstants}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmEmpLogConstants {

    private MdmEmpLogConstants() {
    }

    /**
     * MDM 人员子域日志业务标识（所有 Service / Manager / Controller 类共用，
     * 日志输出统一使用中文方括号【人员主数据】前缀）
     */
    public static final String LOG_BIZ = "人员主数据";
}
