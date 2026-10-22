package com.cacch.integration.common.constant.keruyun;

import lombok.Getter;

/**
 * 客如云订单同步任务状态枚举
 *
 * <p>用于 {@code stg_keruyun_order_sync_task} 控制表的状态字段，驱动任务的执行、断点续传、
 * 重试与终态判断。状态流转路径：PENDING → IN_PROGRESS → SUCCESS / FAILED / PARTIAL。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public enum KeruyunOrderSyncStatus {

    /**
     * 待执行：任务已创建，等待执行器拉取
     */
    PENDING("待执行"),

    /**
     * 执行中：正在逐页拉取客如云数据并落库
     */
    IN_PROGRESS("执行中"),

    /**
     * 成功：全部订单落库成功
     */
    SUCCESS("成功"),

    /**
     * 失败：执行过程中出现异常（网络错误、客如云返回异常等），任务中断
     */
    FAILED("失败"),

    /**
     * 部分失败：客如云接口调用成功但存在落库失败的订单，可重试修复
     */
    PARTIAL("部分失败");

    /**
     * 中文描述
     */
    private final String description;

    KeruyunOrderSyncStatus(String description) {
        this.description = description;
    }
}
