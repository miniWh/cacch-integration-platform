package com.cacch.integration.integration.keruyun.client.dto.order;

import com.cacch.integration.integration.keruyun.client.dto.KeruyunBaseResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 客如云订单详情查询响应
 *
 * <p>成功码 {@code code=0}；字段名与客如云文档 JSON 字段逐字一致。
 * 通用字段与成功判定继承 {@link KeruyunBaseResponse}。
 * 仅保留落库所需结构：主单 orderBaseVO、菜品明细 orderItemVoList、
 * 支付明细 openPaymentDetailVoList；其余结构忽略。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeruyunOrderDetailResponse extends KeruyunBaseResponse<KeruyunOrderDetailResponse.Result> {

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
         * 服务消息 ID
         */
        private String messageId;

        /**
         * 订单详情数据
         */
        private DataInfo data;
    }

    /**
     * 订单详情数据
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataInfo {

        /**
         * 主单信息
         */
        private OrderBaseVO orderBaseVO;

        /**
         * 菜品明细列表（含套餐子商品 children）
         */
        private List<OrderItemVo> orderItemVoList;

        /**
         * 支付明细列表
         */
        private List<PaymentDetailVo> openPaymentDetailVoList;
    }

    /**
     * 主单信息（列表接口已落库 stg_keruyun_order，此处仅用于详情响应承载）
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderBaseVO {

        /**
         * 客如云订单 ID
         */
        private String orderId;

        /**
         * 订单状态
         */
        private String orderStatus;

        /**
         * 结账时间
         */
        private String settleTime;
    }

    /**
     * 菜品明细元素
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemVo {

        /**
         * 商品类型：SINGLE_DISH-单菜/SINGLE_SET-套菜/COMBO-套餐/CUSTOMER_SET-客户套餐等
         */
        private String itemType;

        /**
         * 是否赠送
         */
        private Boolean giftFlag;

        /**
         * 是否称重商品
         */
        private Boolean weighFlag;

        /**
         * 是否临时菜
         */
        private Boolean tempFlag;

        /**
         * 是否优惠菜
         */
        private Boolean promoFlag;

        /**
         * 商品大类名称
         */
        private String bigTypeName;

        /**
         * 商品中类名称
         */
        private String midTypeName;

        /**
         * 商品编码
         */
        private String itemCode;

        /**
         * 商品名称
         */
        private String itemName;

        /**
         * 商品售卖状态：NORMAL-销售/DISCARD-退菜
         */
        private String saleStatusType;

        /**
         * 商品售卖状态编码：NORMAL/DISCARD
         */
        private String saleStatusTypeCode;

        /**
         * 客如云商品行 ID，订单内唯一
         */
        private String id;

        /**
         * 父商品行 ID（套餐子商品场景）
         */
        private String parentId;

        /**
         * 出品部门 ID
         */
        private String productionDeptId;

        /**
         * 单位名称
         */
        private String unitName;

        /**
         * 规格
         */
        private String specName;

        /**
         * 规格名称全称
         */
        private String specNameConcat;

        /**
         * 商品原始单价（单位：分，字符串）
         */
        private String itemPrice;

        /**
         * 商品售价（单位：分，字符串）
         */
        private String salePrice;

        /**
         * 做法列表
         */
        private List<PracticeVo> practiceVoList;

        /**
         * 商品备注
         */
        private String itemSubject;

        /**
         * 销售/退菜/赠菜数量（字符串，如 1.0000）
         */
        private String quantity;

        /**
         * 商品销售金额（单位：分，字符串）
         */
        private String itemSaleAmt;

        /**
         * 服务费分摊金额（单位：分，字符串）
         */
        private String extraFeeApportionAmt;

        /**
         * 商品优惠分摊（单位：分，字符串）
         */
        private String itemPromoApportionAmt;

        /**
         * 商品收入（单位：分，字符串）
         */
        private String itemReceivedAmt;

        /**
         * 子商品节点（套餐子商品）
         */
        private List<OrderItemVo> children;

        /**
         * 商品档案 ID
         */
        private String itemId;

        /**
         * 规格商品 ID
         */
        private String itemSkuId;

        /**
         * 菜品退款时间（仅外卖退款有值）
         */
        private String itemRefundTime;

        /**
         * 商品税率（如 1.53%）
         */
        private String taxRate;

        /**
         * 单品税额（单位：分）
         */
        private Long itemActualTaxAmt;

        /**
         * 菜品行税额（单位：分）
         */
        private Long actualTaxAmt;
    }

    /**
     * 菜品做法
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PracticeVo {

        /**
         * 做法名称
         */
        private String practiceName;

        /**
         * 做法金额（单位：分）
         */
        private Long practiceAmt;
    }

    /**
     * 支付明细元素
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentDetailVo {

        /**
         * 客如云支付单号
         */
        private String payDetailNo;

        /**
         * 面额（单位：分，字符串）
         */
        private String faceAmt;

        /**
         * 支付金额（单位：分，字符串）
         */
        private String payAmt;

        /**
         * 商户优惠金额（单位：分，字符串）
         */
        private String shopPromoAmt;

        /**
         * 平台抽佣/服务费（单位：分，字符串）
         */
        private String platformServiceAmt;

        /**
         * 商户实收（单位：分，字符串）
         */
        private String actualReceiveAmt;

        /**
         * 支付方式名称
         */
        private String payMethodName;

        /**
         * 支付状态：PAY_SUCCESS 等
         */
        private String payDetailStatus;

        /**
         * 创建/支出开始时间
         */
        private String payDetailStartTime;

        /**
         * 支付/退款完成时间
         */
        private String payDetailEndTime;

        /**
         * 支付优惠（单位：分，字符串）
         */
        private String payPromoAmt;

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
         * 券模板 ID（平台券项目 ID）
         */
        private String thirdVoucherItemId;

        /**
         * 操作人姓名/收银员
         */
        private String operatorName;

        /**
         * 实际支付金额（单位：分，字符串）
         */
        private String actualPayAmt;

        /**
         * 平台优惠金额（单位：分，字符串）
         */
        private String platformPromoAmt;

        /**
         * 支付方式 ID：-3-现金/-5-微信/-6-支付宝/-128-挂账 等
         */
        private Long payMethodId;

        /**
         * 团购券是否已对账
         */
        private Boolean couponReconcileFlag;

        /**
         * 挂账人 ID（payMethodId=-128 时有值）
         */
        private String creditMemberId;

        /**
         * 挂账人手机号
         */
        private String creditMemberPhoneNo;

        /**
         * 挂账人名称
         */
        private String creditMemberName;

        /**
         * 团购券类型：1-代金券/2-套餐券/3-次卡/4-阶梯次卡
         */
        private Integer couponType;
    }
}
