package com.cacch.integration.integration.keruyun.client.dto.order;

import com.cacch.integration.integration.keruyun.client.dto.KeruyunBaseResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 客如云订单列表查询响应
 *
 * <p>成功码 {@code code=0}；字段名与客如云文档 JSON 字段逐字一致。
 * 通用字段与成功判定继承 {@link KeruyunBaseResponse}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeruyunOrderQueryResponse extends KeruyunBaseResponse<KeruyunOrderQueryResponse.Result> {

    /**
     * 响应数据主体
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
