package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 职位查询响应中的申请表信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobApplyForm {

    /**
     * 申请表 id
     */
    private Long applyFormId;

    /**
     * 申请表类型：NORMAL_PORTAL=普通官网，RECOMMENDATION_PORTAL=内推官网
     */
    private String type;

    /**
     * 申请表适用浏览器类型：pc 或 mobile
     */
    private String device;
}
