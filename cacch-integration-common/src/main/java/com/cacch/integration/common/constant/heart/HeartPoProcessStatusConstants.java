package com.cacch.integration.common.constant.heart;

/**
 * Heart PO 处理状态常量
 *
 * <p>仅声明当前已实现阶段涉及的状态，后续阶段按需补充。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartPoProcessStatusConstants {

    private HeartPoProcessStatusConstants() {
    }

    /**
     * 客户已解析：可取号
     */
    public static final String CUSTOMER_RESOLVED = "CUSTOMER_RESOLVED";

    /**
     * 已生成 CAC PO 流水号，待港口解析
     */
    public static final String SERIAL_NUMBERED = "SERIAL_NUMBERED";

    /**
     * 启运港、目的港对象 ID 已解析回写，可进入建单
     */
    public static final String PORT_RESOLVED = "PORT_RESOLVED";
}
