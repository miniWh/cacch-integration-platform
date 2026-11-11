package com.cacch.integration.common.constant.ihr;

/**
 * IHR 员工域专有常量类
 *
 * <p>承载员工域各接口共享的请求参数枚举、默认值等</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class IhrStaffConstants {

    private IhrStaffConstants() {
    }

    /**
     * 员工在职状态
     */
    public static final String STAFF_STATUS_IN_SERVICE = "IN_SERVICE";

    /**
     * 员工离职状态
     */
    public static final String STAFF_STATUS_QUIT = "QUIT";

    /**
     * 兜底页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 500;
}
