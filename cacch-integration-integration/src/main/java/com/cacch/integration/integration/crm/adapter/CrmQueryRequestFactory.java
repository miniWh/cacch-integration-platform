package com.cacch.integration.integration.crm.adapter;

import com.cacch.integration.common.constant.crm.CrmConstants;
import com.cacch.integration.integration.crm.client.dto.CrmPageQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmQueryFilter;
import com.cacch.integration.integration.crm.client.dto.CrmQueryGroup;

import java.util.ArrayList;
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
     * 按 modify_time 查询订单（对齐现网可用参数）
     *
     * <p>modify_time 的 field_values 必须为<strong>毫秒时间戳字符串</strong>；
     * 操作符使用 {@code GT}（大于起始时间）。若传入结束时间，再追加 {@code LT}。</p>
     *
     * @param page             页码，从 1 开始
     * @param rows             每页条数
     * @param beginEpochMilli  起始毫秒时间戳字符串（必填）
     * @param endEpochMilli    结束毫秒时间戳字符串（可空；有值时与起始组成区间）
     * @return 分页查询请求
     */
    public static CrmPageQueryRequest orderByModifyTime(int page, int rows,
                                                        String beginEpochMilli,
                                                        String endEpochMilli) {
        List<CrmQueryFilter> filters = new ArrayList<>(2);
        filters.add(CrmQueryFilter.builder()
                .fieldKey(CrmConstants.FIELD_MODIFY_TIME)
                .operator("GT")
                .fieldValues(List.of(beginEpochMilli))
                .build());
        if (endEpochMilli != null && !endEpochMilli.isBlank()) {
            filters.add(CrmQueryFilter.builder()
                    .fieldKey(CrmConstants.FIELD_MODIFY_TIME)
                    .operator("LT")
                    .fieldValues(List.of(endEpochMilli))
                    .build());
        }
        // 单条件时与现网 ESB 一致用 OR；双条件用 AND
        String connector = filters.size() == 1 ? "OR" : "AND";
        return CrmPageQueryRequest.builder()
                .page(String.valueOf(page))
                .rows(String.valueOf(rows))
                .queryGroup(List.of(CrmQueryGroup.builder()
                        .connector(connector)
                        .filters(filters)
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
                .queryGroup(List.of(CrmQueryGroup.builder()
                        .connector("OR")
                        .filters(List.of(CrmQueryFilter.builder()
                                .fieldKey(CrmConstants.FIELD_ORDER_ID)
                                .operator("EQ")
                                .fieldValues(List.of(crmOrderId))
                                .build()))
                        .build()))
                .build();
    }
}
