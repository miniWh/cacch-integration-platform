package com.cacch.integration.integration.keruyun.client.dto.order;

import lombok.Data;

import java.util.List;

/**
 * 客如云订单列表查询响应
 *
 * <p>成功码 {@code code=0}；字段名与客如云文档 JSON 字段逐字一致。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderQueryResponse {

    /**
     * 返回编码，0 表示成功
     */
    private Integer code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 消息 UUID，便于与客如云排查问题
     */
    private String messageUuid;

    /**
     * 响应数据，code != 0 时不返回业务参数
     */
    private Result result;

    /**
     * 是否成功（code == 0）
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return code != null && code == 0;
    }

    /**
     * 响应数据主体
     */
    @Data
    public static class Result {

        /**
         * 是否成功
         */
        private Boolean success;

        /**
         * 服务编码
         */
        private Integer serverCode;

        /**
         * 分页数据
         */
        private DataInfo data;

        /**
         * 服务消息 ID
         */
        private String messageId;

        /**
         * 异常信息
         */
        private String subMsg;
    }

    /**
     * 订单分页数据
     */
    @Data
    public static class DataInfo {

        /**
         * 总条数
         */
        private String totalCount;

        /**
         * 页号
         */
        private String pageNo;

        /**
         * 页大小
         */
        private String pageSize;

        /**
         * 总页数
         */
        private String totalPage;

        /**
         * 订单列表
         */
        private List<KeruyunOrderItem> list;

        /**
         * 上一页
         */
        private String prevPage;

        /**
         * 下一页
         */
        private String nextPage;

        /**
         * 当前 list 是否为 null
         */
        private Boolean emptyForList;
    }
}
