package com.cacch.integration.common.constant.mdm;

/**
 * 部门主数据（MDM）部门子域 —— 日志域常量
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

    /**
     * 日志动作工具方法 —— 构造「动作描述」短字符串
     *
     * <p>当前仅透传，保留便于后续统一调整日志动作命名规范。</p>
     *
     * @param action 动作描述（如 "iHR部门拉取落库"）
     * @return 标准化后的动作描述
     */
    public static String buildAction(String action) {
        return action;
    }
}
