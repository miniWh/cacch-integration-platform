package com.cacch.integration.manager.keruyun.convert;

import com.cacch.integration.common.constant.keruyun.KeruyunOrderConstants;
import com.cacch.integration.entity.keruyun.KeruyunOrderItemDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderPaymentDO;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderDetailResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 客如云订单详情响应 → 落库实体转换器
 *
 * <p>金额字段为分（字符串）→ BIGINT；数量为字符串（如 1.0000）→ NUMERIC；
 * 时间字段为 {@code yyyy-MM-dd HH:mm:ss} → LocalDateTime。
 * 解析失败时按 null 落库并打印 WARN，不阻断整批同步。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface KeruyunOrderDetailConverter {

    /**
     * 日志器（接口字段隐式为 public static final）
     */
    Logger LOG = LoggerFactory.getLogger(KeruyunOrderDetailConverter.class);

    /**
     * 秒级时间格式
     */
    DateTimeFormatter SECOND_FORMATTER = DateTimeFormatter.ofPattern(KeruyunOrderConstants.DATE_PATTERN_SECOND);

    /**
     * 日期格式
     */
    DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern(KeruyunOrderConstants.DATE_PATTERN_DAY);

    /**
     * 菜品明细 → 落库实体（主键、orderId 与审计字段由调用方填充）
     *
     * @param vo 客如云菜品明细元素
     * @return 落库实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "itemRowId", source = "id")
    @Mapping(target = "parentItemRowId", source = "parentId")
    @Mapping(target = "quantity", expression = "java(parseQuantity(vo.getQuantity()))")
    @Mapping(target = "itemPrice", expression = "java(parseAmount(vo.getItemPrice()))")
    @Mapping(target = "salePrice", expression = "java(parseAmount(vo.getSalePrice()))")
    @Mapping(target = "itemSaleAmt", expression = "java(parseAmount(vo.getItemSaleAmt()))")
    @Mapping(target = "extraFeeApportionAmt", expression = "java(parseAmount(vo.getExtraFeeApportionAmt()))")
    @Mapping(target = "itemPromoApportionAmt", expression = "java(parseAmount(vo.getItemPromoApportionAmt()))")
    @Mapping(target = "itemReceivedAmt", expression = "java(parseAmount(vo.getItemReceivedAmt()))")
    @Mapping(target = "practiceInfo", expression = "java(flattenPractice(vo.getPracticeVoList()))")
    @Mapping(target = "itemRefundTime", expression = "java(parseDateTime(vo.getItemRefundTime()))")
    KeruyunOrderItemDO toItemDO(KeruyunOrderDetailResponse.OrderItemVo vo);

    /**
     * 支付明细 → 落库实体（主键、orderId 与审计字段由调用方填充）
     *
     * @param vo 客如云支付明细元素
     * @return 落库实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "faceAmt", expression = "java(parseAmount(vo.getFaceAmt()))")
    @Mapping(target = "payAmt", expression = "java(parseAmount(vo.getPayAmt()))")
    @Mapping(target = "actualPayAmt", expression = "java(parseAmount(vo.getActualPayAmt()))")
    @Mapping(target = "actualReceiveAmt", expression = "java(parseAmount(vo.getActualReceiveAmt()))")
    @Mapping(target = "shopPromoAmt", expression = "java(parseAmount(vo.getShopPromoAmt()))")
    @Mapping(target = "payPromoAmt", expression = "java(parseAmount(vo.getPayPromoAmt()))")
    @Mapping(target = "platformPromoAmt", expression = "java(parseAmount(vo.getPlatformPromoAmt()))")
    @Mapping(target = "platformServiceAmt", expression = "java(parseAmount(vo.getPlatformServiceAmt()))")
    @Mapping(target = "payDetailStartTime", expression = "java(parseDateTime(vo.getPayDetailStartTime()))")
    @Mapping(target = "payDetailEndTime", expression = "java(parseDateTime(vo.getPayDetailEndTime()))")
    KeruyunOrderPaymentDO toPaymentDO(KeruyunOrderDetailResponse.PaymentDetailVo vo);

    /**
     * 金额解析：分（字符串）→ Long
     *
     * @param raw 客如云返回的金额字符串
     * @return 金额（分）；为空或非数字时返回 null
     */
    default Long parseAmount(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            LOG.warn("【客如云订单详情】金额解析失败按 null 落库, raw={}", raw);
            return null;
        }
    }

    /**
     * 数量解析：字符串（如 1.0000）→ BigDecimal（保留 4 位小数）
     *
     * @param raw 客如云返回的数量字符串
     * @return 数量；为空或非数字时返回 null
     */
    default BigDecimal parseQuantity(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim()).setScale(4, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            LOG.warn("【客如云订单详情】数量解析失败按 null 落库, raw={}", raw);
            return null;
        }
    }

    /**
     * 做法列表扁平化为字符串，格式：做法名:金额(分)，分号分隔
     *
     * @param practices 做法列表，可为 null
     * @return 扁平化做法信息；无做法时返回 null
     */
    default String flattenPractice(List<KeruyunOrderDetailResponse.PracticeVo> practices) {
        if (practices == null || practices.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (KeruyunOrderDetailResponse.PracticeVo practice : practices) {
            if (!sb.isEmpty()) {
                sb.append(";");
            }
            sb.append(practice.getPracticeName())
                    .append(":")
                    .append(practice.getPracticeAmt() == null ? 0 : practice.getPracticeAmt());
        }
        return sb.toString();
    }

    /**
     * 时间解析：{@code yyyy-MM-dd HH:mm:ss} 优先，其次 {@code yyyy-MM-dd}
     *
     * @param raw 客如云返回的时间字符串
     * @return 解析结果；为空或无法解析时返回 null
     */
    default LocalDateTime parseDateTime(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        try {
            return LocalDateTime.parse(value, SECOND_FORMATTER);
        } catch (Exception ignore) {
            // 兼容仅返回日期的情况，继续尝试日期格式
        }
        try {
            return LocalDate.parse(value, DAY_FORMATTER).atStartOfDay();
        } catch (Exception e) {
            LOG.warn("【客如云订单详情】时间解析失败按 null 落库, raw={}", raw);
            return null;
        }
    }
}
