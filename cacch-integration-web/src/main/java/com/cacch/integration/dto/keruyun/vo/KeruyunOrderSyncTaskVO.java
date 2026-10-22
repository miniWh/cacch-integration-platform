package com.cacch.integration.dto.keruyun.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客如云订单同步任务视图对象
 *
 * <p>供任务状态查询接口返回，字段与控制表一致，不含敏感信息。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderSyncTaskVO {

    private Long id;
    private LocalDate syncDate;
    private String dateType;
    private LocalDateTime syncWindowStart;
    private LocalDateTime syncWindowEnd;
    private String status;
    private Integer retryCount;
    private Integer lastPage;
    private Integer totalPage;
    private Integer totalCount;
    private Integer successCount;
    private Integer failureCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime syncedAt;
}
