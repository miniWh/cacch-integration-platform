package com.cacch.integration.common.constant.mdm;

/**
 * 主数据（MDM）域级日志常量 —— 人员 / 部门 / 岗位子域共用
 *
 * <p>承载跨子域共享的日志工具方法；各子域日志业务标识（LOG_BIZ）保留在
 * {@code Mdm{Sub}LogConstants} 中。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmLogConstants {

    private MdmLogConstants() {
    }

    /**
     * 日志动作工具方法 —— 构造「动作描述」短字符串
     *
     * <p>当前仅透传，保留便于后续统一调整日志动作命名规范。</p>
     *
     * @param action 动作描述（如 "iHR人员拉取落库"）
     * @return 标准化后的动作描述
     */
    public static String buildAction(String action) {
        return action;
    }
}
