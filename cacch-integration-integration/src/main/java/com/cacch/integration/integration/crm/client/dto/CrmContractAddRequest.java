package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 勤策新增销售合同请求参数
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmContractAddRequest {

    /**
     * 是否触发工作流，默认 false
     */
    private Boolean isTrigger;

    /**
     * 触发员工 ID；isTrigger=true 时必填
     */
    private Long triggerEmployee;

    /**
     * 是否校验验证规则，默认 false
     */
    private Boolean checkRule;

    /**
     * 客户 ID；与 customerCode 至少传一个，优先使用 customerId
     */
    private Long customerId;

    /**
     * 客户编号；与 customerId 至少传一个
     */
    private String customerCode;

    /**
     * 合同日期，格式 yyyy-MM-dd
     */
    private String contractDate;

    /**
     * 合同标题
     */
    private String contractTitle;

    /**
     * 合同生效日期，格式 yyyy-MM-dd
     */
    private String validStartDate;

    /**
     * 合同失效日期，格式 yyyy-MM-dd
     */
    private String validEndDate;

    /**
     * 报价单 ID；与 quotationCode 二选一，优先使用 quotationId
     */
    private Long quotationId;

    /**
     * 报价单编号
     */
    private String quotationCode;

    /**
     * 商机 ID；与 opportunitiesInfoCode 二选一，优先使用 opportunitiesInfoId
     */
    private Long opportunitiesInfoId;

    /**
     * 商机编号
     */
    private String opportunitiesInfoCode;

    /**
     * 币种代码，如 CNY；不传则使用本位币
     */
    private String currencyType;

    /**
     * 主对象自定义字段，格式 {字段key: 字段值}
     */
    private Map<String, Object> extFields;

    /**
     * 自定义从对象数据，格式 {对象key: [{字段key: 字段值}]}
     */
    private Map<String, Object> extSlaveList;

    /**
     * 合同明细列表，至少一条
     */
    private List<CrmContractDetailItem> detailList;
}
