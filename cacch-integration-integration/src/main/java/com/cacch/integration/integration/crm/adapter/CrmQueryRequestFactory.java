package com.cacch.integration.integration.crm.adapter;

import com.cacch.integration.common.constant.crm.CrmConstants;
import com.cacch.integration.integration.crm.client.dto.CrmPageQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmQueryFilter;
import com.cacch.integration.integration.crm.client.dto.CrmQueryGroup;
import com.cacch.integration.integration.crm.client.dto.CrmSortItem;

import java.util.List;

/**
 * 勤策查询请求组装（订单 / 明细常用过滤条件）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmQueryRequestFactory {

    private CrmQueryRequestFactory() {
    }

    /**
     * 按 modify_time 闭区间查询订单（默认按 create_time 倒序）
     *
     * @param page      页码，从 1 开始
     * @param rows      每页条数
     * @param beginTime 开始时间，格式建议 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间，格式建议 yyyy-MM-dd HH:mm:ss
     * @return 分页查询请求
     */
    public static CrmPageQueryRequest orderByModifyTime(int page, int rows, String beginTime, String endTime) {
        return CrmPageQueryRequest.builder()
                .page(String.valueOf(page))
                .rows(String.valueOf(rows))
                .sorts(List.of(CrmSortItem.builder()
                        .fieldKey(CrmConstants.FIELD_CREATE_TIME)
                        .type("desc")
                        .build()))
                .queryGroup(List.of(CrmQueryGroup.builder()
                        .connector("AND")
                        .filters(List.of(
                                CrmQueryFilter.builder()
                                        .fieldKey(CrmConstants.FIELD_MODIFY_TIME)
                                        .operator("GE")
                                        .fieldValues(List.of(beginTime))
                                        .build(),
                                CrmQueryFilter.builder()
                                        .fieldKey(CrmConstants.FIELD_MODIFY_TIME)
                                        .operator("LE")
                                        .fieldValues(List.of(endTime))
                                        .build()
                        ))
                        .build()))
                .build();
    }

    /**
     * 按 CRM 订单 ID 查询明细
     *
     * @param page       页码
     * @param rows       每页条数
     * @param crmOrderId CRM 订单内部 ID（order.id）
     * @return 分页查询请求
     */
    public static CrmPageQueryRequest detailByOrderId(int page, int rows, String crmOrderId) {
        return CrmPageQueryRequest.builder()
                .page(String.valueOf(page))
                .rows(String.valueOf(rows))
                .sorts(List.of(CrmSortItem.builder()
                        .fieldKey(CrmConstants.FIELD_CREATE_TIME)
                        .type("desc")
                        .build()))
                .queryGroup(List.of(CrmQueryGroup.builder()
                        .connector("AND")
                        .filters(List.of(CrmQueryFilter.builder()
                                .fieldKey(CrmConstants.FIELD_ORDER_ID)
                                .operator("EQ")
                                .fieldValues(List.of(crmOrderId))
                                .build()))
                        .build()))
                .build();
    }
}
