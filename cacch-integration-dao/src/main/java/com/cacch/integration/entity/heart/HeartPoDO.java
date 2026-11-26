package com.cacch.integration.entity.heart;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.integration.dao.typehandler.PostgreSqlJsonbTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Heart 客户 PO 订单数据 DO
 *
 * <p>列结构对齐台账「5. SR」，PO 级字段 + 小计汇总 + 流程状态 + CRM 报文本地快照。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName(value = "t_integration_crm_heart_po", autoResultMap = true)
public class HeartPoDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * PO 号（业务幂等键）
     */
    private String poNo;

    /**
     * CAC PO（CRM 取号流水号）
     */
    private String cacPoNo;

    private String customerCode;

    private String customerName;

    private String tradeTerm;

    private String destinationCountry;

    private String portOfLoading;

    private String portOfDischarge;

    /**
     * 启运港 CRM 对象 ID（object_yEQGF__c）
     */
    private String portOfLoadingId;

    /**
     * 目的港 CRM 对象 ID（object_yEQGF__c）
     */
    private String portOfDischargeId;

    /**
     * 目的地名称（查询关键字，来源邮件解析；为空时使用固定值）
     */
    private String destinationName;

    /**
     * 目的地 CRM 对象 ID（object_65Zui__c）
     */
    private String destinationId;

    private String shippingLine;

    private String containerType;

    private String packingType;

    private LocalDate etd;

    private String remark;

    /**
     * 箱数合计
     */
    private BigDecimal totalCartons;

    /**
     * L 合计
     */
    private BigDecimal totalLiters;

    /**
     * 体积合计
     */
    private BigDecimal totalVolume;

    /**
     * 总价合计
     */
    private BigDecimal totalAmount;

    /**
     * 费用总计合计
     */
    private BigDecimal totalCost;

    /**
     * 毛利合计
     */
    private BigDecimal totalGrossProfit;

    /**
     * 毛利率（小数，如 0.0316）
     */
    private BigDecimal grossMarginRate;

    private BigDecimal settlementUnitPrice;

    private String saAux;

    private String saNo;

    private BigDecimal usdGrossProfit;

    private BigDecimal usdAssessPrice;

    private String currency;

    /**
     * 来源邮件记录 ID（t_integration_heart_mail.id）
     */
    private Long mailId;

    /**
     * CRM 可达的 PO 附件 URL
     */
    private String attachmentUrl;

    /**
     * AI 解析置信度（0~1）
     */
    private BigDecimal parseConfidence;

    /**
     * AI 解析原始 JSON
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object parsePayload;

    /**
     * 阶段状态
     */
    private String processStatus;

    /**
     * 最终提交 CRM 的 contractAdd 请求报文快照
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object crmPayload;

    /**
     * 累计提交 CRM 次数
     */
    private Integer submitCount;

    private LocalDateTime lastSubmitAt;

    private String failReason;

    /**
     * 累计重试次数
     */
    private Integer retryCount;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
