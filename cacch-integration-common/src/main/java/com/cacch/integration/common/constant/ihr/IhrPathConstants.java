package com.cacch.integration.common.constant.ihr;

/**
 * IHR 开放平台各接口 PATH 常量类
 *
 * <p>纯接口路径常量，按三维度拆分规范独立成类；所有值均为相对路径，
 * 实际请求 URL 由调用方拼接 {@link com.cacch.integration.common.config.ihr.IhrProperties#getBaseUrl()}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class IhrPathConstants {

    private IhrPathConstants() {
    }

    /**
     * 获取员工 ID 清单
     */
    public static final String STAFF_IDS_LIST_PATH = "/openapi/thirdparty/api/staff/v1/staffs/ids";

    /**
     * 获取员工基本信息
     */
    public static final String STAFF_BASIC_PATH = "/openapi/thirdparty/api/staff/v1/staffs/%s/basic";

    /**
     * 获取当日新增员工 ID 清单
     */
    public static final String STAFF_ADDITIONS_PATH = "/openapi/thirdparty/api/staff/v1/staffs/additions";

    /**
     * 获取当日更新员工 ID 清单 V2
     */
    public static final String STAFF_MODIFICATIONS_PATH = "/openapi/thirdparty/api/staff/v2/staffs/modifications";
}
