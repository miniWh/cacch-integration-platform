package com.cacch.integration.entity.keruyun;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客如云订单支付明细落库实体，映射表 stg_keruyun_order_payment
 *
 * <p>字段对应订单详情接口 openPaymentDetailVoList 元素，金额单位为分。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("stg_keruyun_order_payment")
public class KeruyunOrderPaymentDO {

    /**
     * 内部主键（雪花生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 客如云订单 ID，关联 stg_keruyun_order.order_id
     */
    private String orderId;

    /**
     * 客如云支付单号，订单内唯一
     */
    private String payDetailNo;

    /**
     * 支付方式 ID：-3-现金/-5-微信/-6-支付宝/-128-挂账 等
     */
    private Long payMethodId;

    /**
     * 支付方式名称
     */
    private String payMethodName;

    /**
     * 支付状态：PAY_SUCCESS 等
     */
    private String payDetailStatus;

    /**
     * 面额（单位：分）
     */
    private Long faceAmt;

    /**
     * 支付金额（单位：分）
     */
    private Long payAmt;

    /**
     * 实际支付金额（单位：分）
     */
    private Long actualPayAmt;

    /**
     * 商户实收（单位：分）
     */
    private Long actualReceiveAmt;

    /**
     * 商户优惠金额（单位：分）
     */
    private Long shopPromoAmt;

    /**
     * 支付优惠（单位：分）
     */
    private Long payPromoAmt;

    /**
     * 平台优惠金额（单位：分）
     */
    private Long platformPromoAmt;

    /**
     * 平台抽佣/服务费（单位：分）
     */
    private Long platformServiceAmt;

    /**
     * 券数量
     */
    private String couponCnt;

    /**
     * 券名称
     */
    private String couponName;

    /**
     * 券码，多张券时以英文逗号分隔
     */
    private String couponCode;

    /**
     * 团购券类型：1-代金券/2-套餐券/3-次卡/4-阶梯次卡
     */
    private Integer couponType;

    /**
     * 团购券是否已对账
     */
    private Boolean couponReconcileFlag;

    /**
     * 券模板 ID（平台券项目 ID）
     */
    private String thirdVoucherItemId;

    /**
     * 支付创建/支出开始时间
     */
    private LocalDateTime payDetailStartTime;

    /**
     * 支付/退款完成时间
     */
    private LocalDateTime payDetailEndTime;

    /**
     * 操作人姓名/收银员
     */
    private String operatorName;

    /**
     * 挂账人 ID（payMethodId=-128 时有值）
     */
    private String creditMemberId;

    /**
     * 挂账人名称
     */
    private String creditMemberName;

    /**
     * 挂账人手机号
     */
    private String creditMemberPhoneNo;

    /**
     * 记录创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记：0-正常 1-删除
     */
    @TableLogic
    private Integer isDeleted;
}
