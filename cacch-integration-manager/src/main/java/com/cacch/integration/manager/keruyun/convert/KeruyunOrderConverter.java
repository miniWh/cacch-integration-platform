package com.cacch.integration.manager.keruyun.convert;

import com.cacch.integration.common.constant.keruyun.KeruyunOrderConstants;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import com.cacch.integration.integration.keruyun.client.dto.order.KeruyunOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 客如云订单响应 → 落库实体转换器
 *
 * <p>金额字段为分（字符串）→ BIGINT；时间字段为 {@code yyyy-MM-dd HH:mm:ss} → LocalDateTime。
 * 解析失败时按 null 落库并打印 WARN，不阻断整批同步。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface KeruyunOrderConverter {

    /**
     * 日志器（接口字段隐式为 public static final）
     */
    Logger LOG = LoggerFactory.getLogger(KeruyunOrderConverter.class);

    /**
     * 秒级时间格式
     */
    DateTimeFormatter SECOND_FORMATTER = DateTimeFormatter.ofPattern(KeruyunOrderConstants.DATE_PATTERN_SECOND);

    /**
     * 日期格式
     */
    DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern(KeruyunOrderConstants.DATE_PATTERN_DAY);

    /**
     * 客如云订单元素 → 落库实体（主键、同步批次、审计字段由调用方填充）
     *
     * @param item 客如云订单列表元素
     * @return 落库实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "syncBatch", ignore = true)
    @Mapping(target = "syncedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "orderAmt", expression = "java(parseAmount(item.getOrderAmt()))")
    @Mapping(target = "promoAmt", expression = "java(parseAmount(item.getPromoAmt()))")
    @Mapping(target = "orderReceivedAmt", expression = "java(parseAmount(item.getOrderReceivedAmt()))")
    @Mapping(target = "openTime", expression = "java(parseDateTime(item.getOpenTime()))")
    @Mapping(target = "finishTime", expression = "java(parseDateTime(item.getFinishTime()))")
    KeruyunOrderDO toDO(KeruyunOrderItem item);

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
            LOG.warn("【客如云订单】金额解析失败按 null 落库, raw={}", raw);
            return null;
        }
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
            LOG.warn("【客如云订单】时间解析失败按 null 落库, raw={}", raw);
            return null;
        }
    }
}
