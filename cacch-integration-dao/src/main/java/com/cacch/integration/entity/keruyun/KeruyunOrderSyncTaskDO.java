package com.cacch.integration.entity.keruyun;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客如云订单同步任务控制表实体
 *
 * <p>对应表 {@code stg_keruyun_order_sync_task}，按天粒度管理同步任务的创建、执行、断点续传与终态。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("stg_keruyun_order_sync_task")
public class KeruyunOrderSyncTaskDO {

    /**
     * 内部主键（雪花生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 同步日期（按天），与 date_type 组成唯一键防止重复创建
     */
    private LocalDate syncDate;

    /**
     * 查询时间类型：FINISH_BUSI_DATE / OPEN_TIME / FINISH_TIME / SETTLE_TIME
     */
    private String dateType;

    /**
     * 实际查询窗口起始（sync_date 00:00:00）
     */
    private LocalDateTime syncWindowStart;

    /**
     * 实际查询窗口结束（sync_date 23:59:59）
     */
    private LocalDateTime syncWindowEnd;

    /**
     * 任务状态：PENDING / IN_PROGRESS / SUCCESS / FAILED / PARTIAL
     */
    private String status;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 断点续传用，已拉取到的最后页码，执行时从 last_page+1 开始
     */
    private Integer lastPage;

    /**
     * 总页数（首次执行拉取到第一页后回填）
     */
    private Integer totalPage;

    /**
     * 总订单数（首次执行拉取到第一页后回填）
     */
    private Integer totalCount;

    /**
     * 落库成功数
     */
    private Integer successCount;

    /**
     * 落库失败数
     */
    private Integer failureCount;

    /**
     * 失败原因（截断至 500 字符）
     */
    private String errorMessage;

    /**
     * 乐观锁版本号，状态流转时 CAS 更新
     */
    private Integer version;

    /**
     * 任务创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 任务最近状态/进度更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 任务执行完成时间
     */
    private LocalDateTime syncedAt;

    /**
     * 逻辑删除：0-正常 1-删除
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
