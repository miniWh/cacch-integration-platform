package com.cacch.integration.integration.keruyun.client.dto.order;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客如云订单查询分页参数
 *
 * <p>客如云文档中 pageNum / pageSize 均为字符串类型；文档对两者含义的描述疑似颠倒，
 * 本项目按通用语义使用：pageNum=页码（从 1 开始），pageSize=每页条数。</p>
 *
 * <p>字段顺序通过 {@link JsonPropertyOrder} 固定，与 ESB 侧报文顺序保持一致。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"pageNum", "pageSize"})
public class KeruyunPageBean {

    /**
     * 页码，从 1 开始
     */
    private String pageNum;

    /**
     * 每页条数
     */
    private String pageSize;
}
