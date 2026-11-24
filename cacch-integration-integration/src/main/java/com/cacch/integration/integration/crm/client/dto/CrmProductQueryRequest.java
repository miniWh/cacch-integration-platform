package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 勤策查询商品请求参数
 *
 * <p>全部字段均为可选，未赋值的字段不会出现在请求体与签名串中。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmProductQueryRequest {

    /**
     * 页码，从 1 开始；为空时服务端默认 1
     */
    @JsonProperty("page_number")
    @JsonAlias("pageNumber")
    private Integer pageNumber;

    /**
     * 商品唯一标识（来源第三方系统的唯一 ID）
     */
    @JsonProperty("prd_id")
    @JsonAlias("prdId")
    private String prdId;

    /**
     * 商品编码
     */
    @JsonProperty("prd_code")
    @JsonAlias("prdCode")
    private String prdCode;

    /**
     * 创建时间下限，格式 yyyy-MM-dd HH:mm:ss
     */
    @JsonProperty("after_create_time")
    @JsonAlias("afterCreateTime")
    private String afterCreateTime;

    /**
     * 创建时间上限，格式 yyyy-MM-dd HH:mm:ss
     */
    @JsonProperty("before_create_time")
    @JsonAlias("beforeCreateTime")
    private String beforeCreateTime;

    /**
     * 修改时间下限，格式 yyyy-MM-dd HH:mm:ss
     */
    @JsonProperty("after_modify_time")
    @JsonAlias("afterModifyTime")
    private String afterModifyTime;

    /**
     * 修改时间上限，格式 yyyy-MM-dd HH:mm:ss
     */
    @JsonProperty("before_modify_time")
    @JsonAlias("beforeModifyTime")
    private String beforeModifyTime;

    /**
     * 品牌 ID；与品牌名称同时存在时优先使用 ID
     */
    @JsonProperty("brand_id")
    @JsonAlias("brandId")
    private String brandId;

    /**
     * 品牌名称
     */
    @JsonProperty("brand_name")
    @JsonAlias("brandName")
    private String brandName;

    /**
     * 商品分类，多个用英文逗号分隔：0普通商品、1助销物料、2包装物、3兑换物料
     */
    @JsonProperty("classification")
    private String classification;
}
