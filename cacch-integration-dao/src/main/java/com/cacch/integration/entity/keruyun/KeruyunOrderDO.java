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
 * 客如云订单落库实体，映射表 stg_keruyun_order
 *
 * <p>字段对应客如云订单列表查询接口返回的订单元素，金额单位为分。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("stg_keruyun_order")
public class KeruyunOrderDO {

    /**
     * 内部主键（雪花生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 品牌 ID
     */
    private String brandId;

    /**
     * 门店 ID
     */
    private String shopId;

    /**
     * 客如云订单 ID，业务唯一键
     */
    private String orderId;

    /**
     * 业务订单号
     */
    private String busiOrderNo;

    /**
     * 订单状态，取值见客如云订单状态枚举
     */
    private String orderStatus;

    /**
     * 订单类型，取值见客如云订单类型枚举
     */
    private String orderType;

    /**
     * 订单金额（单位：分）
     */
    private Long orderAmt;

    /**
     * 优惠金额（单位：分）
     */
    private Long promoAmt;

    /**
     * 订单收入/订单实收（单位：分）
     */
    private Long orderReceivedAmt;

    /**
     * 下单时间
     */
    private LocalDateTime openTime;

    /**
     * 完结时间
     */
    private LocalDateTime finishTime;

    /**
     * 第三方订单号
     */
    private String thirdOrderNo;

    /**
     * 订单流水号
     */
    private String serialNo;

    /**
     * 第三方流水号
     */
    private String thirdSerialNo;

    /**
     * 订单模式标签
     */
    private String orderModeTag;

    /**
     * 订单模式
     */
    private String orderMode;

    /**
     * 同步批次号（便于追溯）
     */
    private String syncBatch;

    /**
     * 最近一次从客如云同步成功的时间
     */
    private LocalDateTime syncedAt;

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
